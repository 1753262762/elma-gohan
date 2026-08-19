package com.elma.gohan.domain.risk;

import com.elma.gohan.config.RiskProperties;
import com.elma.gohan.domain.restaurant.Restaurant;
import com.elma.gohan.provider.evidence.EvidenceStatus;
import com.elma.gohan.provider.evidence.RestaurantEvidence;
import com.elma.gohan.provider.evidence.ReviewEvidence;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** risk-v0.2：基础评分、评论模板、时间突发、近期趋势和数据不足的透明规则模型。 */
@Component
public class RuleBasedRiskEngine implements RiskEngine {

    private final RiskProperties properties;
    private final TemplateCommentDetector templateDetector;
    private final ReviewBurstDetector burstDetector;
    private final RecentTrendDetector trendDetector;

    @Autowired
    public RuleBasedRiskEngine(RiskProperties properties,
                               TemplateCommentDetector templateDetector,
                               ReviewBurstDetector burstDetector,
                               RecentTrendDetector trendDetector) {
        this.properties = properties;
        this.templateDetector = templateDetector;
        this.burstDetector = burstDetector;
        this.trendDetector = trendDetector;
    }

    /** 便于纯单元测试使用默认轻量分析器。 */
    public RuleBasedRiskEngine(RiskProperties properties) {
        this(properties, new JaccardTemplateCommentDetector(properties),
                new SlidingWindowBurstDetector(properties),
                new RuleBasedRecentTrendDetector(properties));
    }

    @Override
    public RiskResult evaluate(Restaurant restaurant, RestaurantEvidence suppliedEvidence) {
        RestaurantEvidence evidence = suppliedEvidence == null
                ? RestaurantEvidence.unavailable("UNKNOWN") : suppliedEvidence;
        Set<String> reasons = new LinkedHashSet<>();

        int ratingRisk = ratingRisk(restaurant, reasons);
        TemplateDetectionResult template = templateDetector.detect(evidence.reviews());
        int templateRisk = linearRisk(template.templateRatio(),
                properties.getTemplate().getRatioStart(), properties.getTemplate().getRatioFull());
        if (templateRisk > 0) reasons.add("相似措辞评论偏多");

        BurstDetectionResult burst = burstDetector.detect(evidence.reviews());
        if (burst.burstRisk() > 0) reasons.add("评论在少数日期异常集中");

        RecentTrend trend = trendDetector.detect(evidence.reviews());
        int trendRisk = switch (trend) {
            case UP -> properties.getTrend().getUpRisk();
            case STABLE -> properties.getTrend().getStableRisk();
            case DOWN -> properties.getTrend().getDownRisk();
            case UNKNOWN -> properties.getTrend().getUnknownRisk();
        };
        if (trend == RecentTrend.DOWN) reasons.add("近期口碑较历史明显下降");
        if (trend == RecentTrend.UP) reasons.add("近期口碑有所改善");

        int insufficientRisk = dataInsufficientRisk(restaurant, evidence, reasons);
        RiskFactors factors = new RiskFactors(ratingRisk, templateRisk, burst.burstRisk(),
                trendRisk, insufficientRisk);
        int score = weightedScore(factors);
        double confidence = confidence(restaurant, evidence);

        if (evidence.status() == EvidenceStatus.AVAILABLE && templateRisk == 0
                && burst.burstRisk() == 0 && trend != RecentTrend.DOWN) {
            reasons.add("未发现明显评论异常");
        }
        if (reasons.isEmpty()) reasons.add("现有数据未发现明显风险");
        List<String> visibleReasons = new ArrayList<>(reasons);
        if (visibleReasons.size() > 5) visibleReasons = visibleReasons.subList(0, 5);

        return new RiskResult(score, levelOf(score), confidence, factors,
                visibleReasons, properties.getAlgorithmVersion());
    }

    private int ratingRisk(Restaurant restaurant, Set<String> reasons) {
        RiskProperties.Rating rating = properties.getRating();
        if (restaurant.rating() == null) {
            reasons.add("评分数据缺失");
            return rating.getMissingRisk();
        }
        if (restaurant.rating() >= rating.getExcellentMin()) return rating.getExcellentRisk();
        if (restaurant.rating() >= rating.getGoodMin()) {
            reasons.add("基础评分良好但未达优秀");
            return rating.getGoodRisk();
        }
        if (restaurant.rating() >= rating.getFairMin()) {
            reasons.add("基础评分一般");
            return rating.getFairRisk();
        }
        reasons.add("基础评分偏低");
        return rating.getPoorRisk();
    }

    private int dataInsufficientRisk(Restaurant restaurant, RestaurantEvidence evidence,
                                     Set<String> reasons) {
        RiskProperties.DataInsufficient data = properties.getDataInsufficient();
        int risk;
        if (evidence.status() == EvidenceStatus.UNAVAILABLE) {
            risk = data.getUnavailableRisk();
            reasons.add("外部证据服务暂不可用，风险判断可信度较低");
        } else if (evidence.status() == EvidenceStatus.NO_DATA) {
            risk = data.getNoDataRisk();
            reasons.add("暂无外部评论证据，风险判断可信度较低");
        } else {
            double missingRatio = 1.0 - Math.min(1.0,
                    (double) evidence.reviews().size() / Math.max(1, data.getTargetReviews()));
            risk = (int) Math.round(missingRatio * data.getSampleShortageMaxRisk());
            if (evidence.reviews().size() < data.getTargetReviews()) {
                reasons.add("外部评论样本不足");
            }
        }
        if (restaurant.reviewCount() == null
                || restaurant.reviewCount() < data.getPoiReviewCountThreshold()) {
            risk += data.getReviewCountMissing();
            reasons.add("平台评价数量不足");
        }
        if (restaurant.openingHours() == null || restaurant.openingHours().isBlank()) {
            risk += data.getOpeningHoursMissing();
            reasons.add("营业信息缺失");
        }
        if (restaurant.averagePrice() == null) {
            risk += data.getPriceMissing();
            reasons.add("价格信息缺失");
        } else if (evidence.poolAveragePrice() != null && evidence.poolAveragePrice() > 0
                && restaurant.averagePrice()
                > evidence.poolAveragePrice() * properties.getPriceAnomalyRatio()) {
            risk += data.getPriceAnomaly();
            reasons.add("价格明显高于同批候选");
        }
        return clamp(risk);
    }

    private int weightedScore(RiskFactors factors) {
        RiskProperties.Weights weights = properties.getWeights();
        double score = factors.ratingRisk() * weights.getRating()
                + factors.templateRisk() * weights.getTemplate()
                + factors.burstRisk() * weights.getBurst()
                + factors.trendRisk() * weights.getTrend()
                + factors.dataInsufficientRisk() * weights.getDataInsufficient();
        return clamp((int) Math.round(score));
    }

    private double confidence(Restaurant restaurant, RestaurantEvidence evidence) {
        RiskProperties.Confidence config = properties.getConfidence();
        int complete = 0;
        if (restaurant.rating() != null) complete++;
        if (restaurant.reviewCount() != null) complete++;
        if (restaurant.openingHours() != null && !restaurant.openingHours().isBlank()) complete++;
        if (restaurant.averagePrice() != null) complete++;
        double poi = complete / 4.0;

        double evidenceConfidence = 0.0;
        if (evidence.status() == EvidenceStatus.AVAILABLE && !evidence.reviews().isEmpty()) {
            List<ReviewEvidence> reviews = evidence.reviews();
            double volume = Math.min(1.0,
                    (double) reviews.size() / Math.max(1, config.getTargetReviews()));
            double text = coverage(reviews, r -> r.text() != null && !r.text().isBlank());
            double rating = coverage(reviews, r -> r.rating() != null);
            double time = coverage(reviews, r -> r.createdAt() != null);
            evidenceConfidence = volume * (text + rating + time) / 3.0;
        }
        double value = config.getPoiWeight() * poi
                + config.getEvidenceWeight() * evidenceConfidence;
        return Math.round(Math.max(0.0, Math.min(1.0, value)) * 1000.0) / 1000.0;
    }

    private double coverage(List<ReviewEvidence> reviews,
                            java.util.function.Predicate<ReviewEvidence> predicate) {
        return (double) reviews.stream().filter(predicate).count() / reviews.size();
    }

    private int linearRisk(double value, double start, double full) {
        if (value <= start) return 0;
        if (value >= full) return 100;
        return clamp((int) Math.round((value - start) * 100.0 / (full - start)));
    }

    private RiskLevel levelOf(int score) {
        RiskProperties.Levels levels = properties.getLevels();
        if (score <= levels.getLowMaxInclusive()) return RiskLevel.LOW;
        if (score <= levels.getMediumLowMaxInclusive()) return RiskLevel.MEDIUM_LOW;
        if (score <= levels.getMediumMaxInclusive()) return RiskLevel.MEDIUM;
        return RiskLevel.HIGH;
    }

    private int clamp(int value) { return Math.max(0, Math.min(100, value)); }
}
