package com.elma.gohan.application;

import com.elma.gohan.config.RecommendationProperties;
import com.elma.gohan.controller.api.CreateRecommendationRequest;
import com.elma.gohan.controller.api.FeedbackResponse;
import com.elma.gohan.controller.api.RecommendationResponse;
import com.elma.gohan.controller.api.RestaurantSummary;
import com.elma.gohan.controller.api.RiskAssessment;
import com.elma.gohan.controller.api.SubmitFeedbackRequest;
import com.elma.gohan.domain.recommendation.RecommendationEngine;
import com.elma.gohan.domain.recommendation.RecommendationResult;
import com.elma.gohan.domain.recommendation.RestaurantCandidate;
import com.elma.gohan.domain.restaurant.DataCompleteness;
import com.elma.gohan.domain.restaurant.Location;
import com.elma.gohan.domain.restaurant.Restaurant;
import com.elma.gohan.domain.restaurant.SearchCondition;
import com.elma.gohan.domain.risk.RiskEngine;
import com.elma.gohan.domain.risk.RiskLevel;
import com.elma.gohan.domain.risk.RiskResult;
import com.elma.gohan.infrastructure.persistence.RecommendationCandidateEntity;
import com.elma.gohan.infrastructure.persistence.RecommendationCandidateRepository;
import com.elma.gohan.infrastructure.persistence.RecommendationLogEntity;
import com.elma.gohan.infrastructure.persistence.RecommendationLogRepository;
import com.elma.gohan.infrastructure.persistence.RestaurantEntity;
import com.elma.gohan.infrastructure.persistence.RestaurantRepository;
import com.elma.gohan.infrastructure.persistence.RiskResultEntity;
import com.elma.gohan.infrastructure.persistence.RiskResultRepository;
import com.elma.gohan.infrastructure.persistence.UserFeedbackEntity;
import com.elma.gohan.infrastructure.persistence.UserFeedbackRepository;
import com.elma.gohan.infrastructure.persistence.UserPreferenceEntity;
import com.elma.gohan.infrastructure.persistence.UserPreferenceRepository;
import com.elma.gohan.provider.evidence.EvidenceProvider;
import com.elma.gohan.provider.evidence.RestaurantEvidence;
import com.elma.gohan.provider.poi.PoiProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 推荐编排:POI -> 风险 -> 排序 -> 候选池落库 -> reroll 游标 -> 反馈。
 */
@Service
public class RecommendationService {

    private static final Set<Integer> ALLOWED_RADIUS = Set.of(500, 1000, 2000, 3000);
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final PoiProvider poiProvider;
    private final EvidenceProvider evidenceProvider;
    private final RiskEngine riskEngine;
    private final RecommendationEngine recommendationEngine;
    private final RecommendationProperties recommendationProperties;
    private final RestaurantRepository restaurantRepository;
    private final RiskResultRepository riskResultRepository;
    private final RecommendationLogRepository logRepository;
    private final RecommendationCandidateRepository candidateRepository;
    private final UserFeedbackRepository feedbackRepository;
    private final UserPreferenceRepository preferenceRepository;
    private final ObjectMapper objectMapper;

    public RecommendationService(PoiProvider poiProvider, EvidenceProvider evidenceProvider,
                                 RiskEngine riskEngine, RecommendationEngine recommendationEngine,
                                 RecommendationProperties recommendationProperties,
                                 RestaurantRepository restaurantRepository,
                                 RiskResultRepository riskResultRepository,
                                 RecommendationLogRepository logRepository,
                                 RecommendationCandidateRepository candidateRepository,
                                 UserFeedbackRepository feedbackRepository,
                                 UserPreferenceRepository preferenceRepository,
                                 ObjectMapper objectMapper) {
        this.poiProvider = poiProvider;
        this.evidenceProvider = evidenceProvider;
        this.riskEngine = riskEngine;
        this.recommendationEngine = recommendationEngine;
        this.recommendationProperties = recommendationProperties;
        this.restaurantRepository = restaurantRepository;
        this.riskResultRepository = riskResultRepository;
        this.logRepository = logRepository;
        this.candidateRepository = candidateRepository;
        this.feedbackRepository = feedbackRepository;
        this.preferenceRepository = preferenceRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RecommendationResponse create(UUID anonymousUserId, CreateRecommendationRequest request) {
        if (request.radius() != null && !ALLOWED_RADIUS.contains(request.radius())) {
            throw new ValidationFailedException("radius", "只能是 500、1000、2000 或 3000");
        }
        int radius = request.radius() == null ? 1000 : request.radius();
        if (request.dislikes() != null && request.dislikes().stream().distinct().count()
                != request.dislikes().size()) {
            throw new ValidationFailedException("dislikes", "不能有重复项");
        }
        SearchCondition condition = new SearchCondition(
                radius, request.maxBudget(),
                request.category() == null ? SearchCondition.CATEGORY_ANY : request.category(),
                request.dislikes() == null ? List.of() : request.dislikes());

        List<Restaurant> pois = poiProvider.nearby(
                new Location(request.latitude(), request.longitude()), condition);

        double poolAvgPrice = pois.stream()
                .filter(r -> r.averagePrice() != null)
                .mapToInt(Restaurant::averagePrice)
                .average().orElse(0);
        Map<String, RiskResult> risks = riskEngine.evaluateAll(
                pois, r -> {
                    RestaurantEvidence evidence = evidenceProvider.getEvidence(r);
                    return poolAvgPrice > 0 ? evidence.withPoolAveragePrice(poolAvgPrice) : evidence;
                });

        RecommendationResult result = recommendationEngine.recommend(
                pois, risks, new com.elma.gohan.domain.recommendation.UserPreference(condition));
        if (result.pool().isEmpty()) {
            throw new NoRecommendationAvailableException("附近暂时没有符合条件的餐厅,请放宽距离或预算");
        }

        List<RestaurantCandidate> pool = result.pool();
        LocalDateTime now = LocalDateTime.now(ZONE);
        UUID logId = UUID.randomUUID();

        // upsert restaurant,同时把内部 id 回填到候选
        List<RestaurantCandidate> persisted = new java.util.ArrayList<>(pool.size());
        for (RestaurantCandidate candidate : pool) {
            Restaurant saved = upsertRestaurant(candidate.restaurant(), now);
            riskResultRepository.save(new RiskResultEntity(
                    UUID.randomUUID(), saved.id(), candidate.risk().riskScore(),
                    candidate.risk().riskLevel().name(), toJson(candidate.risk().reasons()),
                    candidate.risk().algorithmVersion(), now));
            persisted.add(new RestaurantCandidate(
                    saved, candidate.risk(), candidate.lowRegretScore(), candidate.reasons()));
        }

        RestaurantCandidate first = persisted.get(0);
        Map<String, Object> conditionSnapshot = new java.util.LinkedHashMap<>();
        conditionSnapshot.put("latitude", request.latitude());
        conditionSnapshot.put("longitude", request.longitude());
        conditionSnapshot.put("radius", radius);
        conditionSnapshot.put("maxBudget", request.maxBudget());
        conditionSnapshot.put("category", condition.category());
        conditionSnapshot.put("dislikes", condition.dislikes());
        logRepository.save(new RecommendationLogEntity(
                logId, anonymousUserId, toJson(conditionSnapshot),
                persisted.size(), first.restaurant().id(),
                first.risk().riskScore(), first.lowRegretScore(),
                first.risk().algorithmVersion(), result.algorithmVersion(), now));

        for (int i = 0; i < persisted.size(); i++) {
            RestaurantCandidate candidate = persisted.get(i);
            candidateRepository.save(new RecommendationCandidateEntity(
                    UUID.randomUUID(), logId, candidate.restaurant().id(), i + 1,
                    candidate.restaurant().distanceMeters(),
                    candidate.risk().riskScore(), candidate.risk().riskLevel().name(),
                    toJson(candidate.risk().reasons()), candidate.risk().algorithmVersion(),
                    candidate.lowRegretScore(), toJson(candidate.reasons()), i == 0));
        }

        preferenceRepository.save(new UserPreferenceEntity(
                UUID.randomUUID(), anonymousUserId, toJson(conditionSnapshot), now));

        return toResponse(logId, persisted.get(0), persisted.size() - 1);
    }

    @Transactional
    public RecommendationResponse reroll(UUID anonymousUserId, UUID recommendationId) {
        RecommendationLogEntity log = findLog(anonymousUserId, recommendationId);
        List<RecommendationCandidateEntity> candidates =
                candidateRepository.findByRecommendationLogIdOrderBySlotAsc(recommendationId);

        RecommendationCandidateEntity target = null;
        int remaining = 0;
        for (RecommendationCandidateEntity c : candidates) {
            if (!c.isShown()) {
                if (target == null) {
                    target = c;
                    c.markShown();
                } else {
                    remaining++;
                }
            }
        }
        if (target == null) {
            // 候选耗尽:回到初始 A,不再产生第四家
            target = candidates.get(0);
            remaining = 0;
        }
        log.updateCurrent(target.getRestaurantId(), target.getRiskScore(), target.getLowRegretScore());

        return toResponse(log.getId(), toView(log.getId(), target), remaining);
    }

    @Transactional
    public FeedbackResponse submitFeedback(UUID anonymousUserId, UUID recommendationId,
                                           SubmitFeedbackRequest request) {
        RecommendationLogEntity log = findLog(anonymousUserId, recommendationId);
        LocalDateTime now = LocalDateTime.now(ZONE);
        UserFeedbackEntity feedback = feedbackRepository.save(new UserFeedbackEntity(
                UUID.randomUUID(), log.getId(), log.getCurrentRestaurantId(), anonymousUserId,
                request.result().name(), now));
        return new FeedbackResponse(
                feedback.getId().toString(), log.getId().toString(),
                feedback.getRestaurantId().toString(), feedback.getResult(),
                now.atZone(ZONE).toString());
    }

    private RecommendationLogEntity findLog(UUID anonymousUserId, UUID recommendationId) {
        return logRepository.findByIdAndAnonymousUserId(recommendationId, anonymousUserId)
                .orElseThrow(() -> new RecommendationNotFoundException("推荐已失效,请重新获取"));
    }

    private Restaurant upsertRestaurant(Restaurant r, LocalDateTime now) {
        RestaurantEntity entity = restaurantRepository
                .findBySourceAndSourcePoiId(r.source(), r.sourcePoiId())
                .orElseGet(() -> new RestaurantEntity(UUID.randomUUID(), r.source(), r.sourcePoiId(),
                        r.name(), r.latitude(), r.longitude(), r.categoryCode(), r.categoryLabel(),
                        r.rating(), r.reviewCount(), r.averagePrice(), r.businessStatus(),
                        r.openingHours(), r.address(), r.dataCompleteness(), now, now));
        RestaurantEntity updated = new RestaurantEntity(
                entity.getId(), entity.getSource(), entity.getSourcePoiId(),
                r.name(), r.latitude(), r.longitude(), r.categoryCode(), r.categoryLabel(),
                r.rating(), r.reviewCount(), r.averagePrice(), r.businessStatus(),
                r.openingHours(), r.address(), r.dataCompleteness(),
                entity.getCreatedAt(), now);
        return toDomain(restaurantRepository.save(updated), r.distanceMeters());
    }

    private Restaurant toDomain(RestaurantEntity e, int distanceMeters) {
        return new Restaurant(e.getId(), e.getSource(), e.getSourcePoiId(), e.getName(),
                e.getLatitude(), e.getLongitude(), distanceMeters, e.getCategoryCode(),
                e.getCategoryLabel(), e.getRating(), e.getReviewCount(), e.getAveragePrice(),
                e.getBusinessStatus(), e.getOpeningHours(), e.getAddress(),
                e.getDataCompleteness() == null ? DataCompleteness.MINIMAL : e.getDataCompleteness());
    }

    /** reroll 时从候选快照 + restaurant 表重建候选视图。 */
    private RestaurantCandidate toView(UUID logId, RecommendationCandidateEntity c) {
        RestaurantEntity e = restaurantRepository.findById(c.getRestaurantId())
                .orElseThrow(() -> new RecommendationNotFoundException("推荐已失效,请重新获取"));
        Restaurant restaurant = toDomain(e, c.getDistanceMeters());
        RiskResult risk = new RiskResult(c.getRiskScore(), RiskLevel.valueOf(c.getRiskLevel()),
                fromJson(c.getRiskReasonsJson()), c.getRiskAlgorithmVersion());
        return new RestaurantCandidate(restaurant, risk, c.getLowRegretScore(),
                fromJson(c.getReasonsJson()));
    }

    private RecommendationResponse toResponse(UUID logId, RestaurantCandidate candidate,
                                              int alternativesRemaining) {
        Restaurant r = candidate.restaurant();
        int walkingMinutes = Math.max(1, (int) Math.ceil(
                r.distanceMeters() / (double) recommendationProperties.getWalkingSpeedMetersPerMinute()));
        return new RecommendationResponse(
                logId.toString(),
                new RestaurantSummary(
                        r.id().toString(), r.name(), r.latitude(), r.longitude(), r.address(),
                        new RestaurantSummary.Category(r.categoryCode(), r.categoryLabel()),
                        r.distanceMeters(), walkingMinutes, r.averagePrice(), r.rating(),
                        r.businessStatus().name()),
                new RiskAssessment(candidate.risk().riskScore(), candidate.risk().riskLevel().name(),
                        candidate.risk().reasons(), candidate.risk().algorithmVersion()),
                candidate.reasons(),
                Math.min(2, Math.max(0, alternativesRemaining)));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON 序列化失败", e);
        }
    }

    private List<String> fromJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() { });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON 反序列化失败", e);
        }
    }
}
