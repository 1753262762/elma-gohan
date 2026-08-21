package com.elma.gohan.domain.recommendation;

import com.elma.gohan.config.RecommendationProperties;
import com.elma.gohan.domain.restaurant.CategoryFilter;
import com.elma.gohan.domain.restaurant.Restaurant;
import com.elma.gohan.domain.restaurant.SearchCondition;
import com.elma.gohan.domain.risk.RiskResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * recommendation-v0.3 默认推荐引擎:
 * 硬过滤 -> 高风险剔除 -> LowRegretScore 排序 -> Top-K 截断 -> 品类轮询加权随机候选池。
 * 多样化只在候选池抽取这一层生效(单层策略);seed 由编排层传入并落库,支持确定性重放。
 */
@Component
public class DefaultRecommendationEngine implements RecommendationEngine {

    private record Scored(Restaurant restaurant, RiskResult risk, double score) { }

    private final HardFilter hardFilter;
    private final LowRegretScorer scorer;
    private final RecommendationProperties props;

    public DefaultRecommendationEngine(HardFilter hardFilter, LowRegretScorer scorer,
                                       RecommendationProperties props) {
        this.hardFilter = hardFilter;
        this.scorer = scorer;
        this.props = props;
    }

    @Override
    public RecommendationResult recommend(List<Restaurant> candidates,
                                          Map<String, RiskResult> risks,
                                          UserPreference preference,
                                          long seed) {
        var condition = preference.condition();
        List<Restaurant> filtered = hardFilter.filter(candidates, condition).stream()
                .filter(r -> !isBlocked(risks.get(r.sourcePoiId())))
                .toList();
        if (filtered.isEmpty()) {
            return new RecommendationResult(List.of(), props.getAlgorithmVersion(), seed, List.of());
        }

        List<Scored> scored = new ArrayList<>();
        for (Restaurant r : filtered) {
            RiskResult risk = risks.get(r.sourcePoiId());
            scored.add(new Scored(r, risk, scorer.score(r, risk, preference)));
        }
        scored.sort(Comparator.comparingDouble(Scored::score).reversed()
                .thenComparing(item -> stableKey(item.restaurant())));
        List<Scored> topK = scored.subList(0, Math.min(props.getTopK(), scored.size()));

        int poolSize = Math.min(props.getPoolSize(), topK.size());
        List<SelectionCandidate> selectionSnapshot = topK.stream()
                .map(item -> new SelectionCandidate(item.restaurant().source(),
                        item.restaurant().sourcePoiId(),
                        diversityKey(item.restaurant(), condition), item.score()))
                .toList();
        List<SelectionCandidate> selected = replaySelection(selectionSnapshot, poolSize, seed);
        Map<String, Scored> scoredByKey = topK.stream().collect(java.util.stream.Collectors.toMap(
                item -> stableKey(item.restaurant()), item -> item));
        List<Scored> picked = selected.stream()
                .map(item -> scoredByKey.get(item.candidateKey()))
                .toList();

        List<RestaurantCandidate> pool = picked.stream()
                .map(s -> new RestaurantCandidate(
                        s.restaurant(), s.risk(), s.score(),
                        scorer.reasons(s.restaurant(), s.risk(), preference)))
                .toList();
        return new RecommendationResult(pool, props.getAlgorithmVersion(), seed, selectionSnapshot);
    }

    /**
     * 只在 Top-K 内抽取;按多样化分组轮询,每组内部按 LowRegretScore 加权且不放回。
     * 这是唯一的多样化层:随机性不会重新引入被过滤项,也不会把均衡候选池抽成单一品类。
     */
    @Override
    public List<SelectionCandidate> replaySelection(List<SelectionCandidate> selectionSnapshot,
                                                    int poolSize, long seed) {
        Map<String, List<SelectionCandidate>> groups = new LinkedHashMap<>();
        for (SelectionCandidate item : selectionSnapshot) {
            groups.computeIfAbsent(item.diversityKey(), ignored -> new ArrayList<>()).add(item);
        }
        WeightedRandomSelector selector = new WeightedRandomSelector(seed);
        List<SelectionCandidate> picked = new ArrayList<>(Math.max(0, poolSize));
        while (picked.size() < poolSize) {
            boolean added = false;
            for (List<SelectionCandidate> group : groups.values()) {
                if (picked.size() >= poolSize || group.isEmpty()) continue;
                List<Double> weights = group.stream()
                        .map(item -> Math.max(1.0, item.lowRegretScore())).toList();
                SelectionCandidate chosen = selector.select(group, weights, 1).get(0);
                group.remove(chosen);
                picked.add(chosen);
                added = true;
            }
            if (!added) break;
        }
        return picked;
    }

    private String stableKey(Restaurant restaurant) {
        return new SelectionCandidate(restaurant.source(), restaurant.sourcePoiId(), "", 0.0)
                .candidateKey();
    }

    private String diversityKey(Restaurant restaurant, SearchCondition condition) {
        if (condition.categoryUnlimited()) {
            return CategoryFilter.groupCodeForRestaurant(restaurant.categoryCode());
        }
        return restaurant.categoryCode() == null
                ? "OTHER"
                : restaurant.categoryCode().toUpperCase(Locale.ROOT);
    }
}
