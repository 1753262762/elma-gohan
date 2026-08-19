package com.elma.gohan.domain.recommendation;

import com.elma.gohan.config.RecommendationProperties;
import com.elma.gohan.domain.restaurant.Restaurant;
import com.elma.gohan.domain.risk.RiskResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * V0.1 默认推荐引擎:
 * 硬过滤 -> 高风险剔除 -> LowRegretScore 排序 -> Top-K -> 加权随机抽取候选池(A/B/C)。
 */
@Component
public class DefaultRecommendationEngine implements RecommendationEngine {

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
                                          UserPreference preference) {
        var condition = preference.condition();
        List<Restaurant> filtered = hardFilter.filter(candidates, condition).stream()
                .filter(r -> !isBlocked(risks.get(r.sourcePoiId())))
                .toList();
        if (filtered.isEmpty()) {
            return new RecommendationResult(List.of(), props.getAlgorithmVersion());
        }

        record Scored(Restaurant restaurant, RiskResult risk, double score) { }
        List<Scored> scored = new ArrayList<>();
        for (Restaurant r : filtered) {
            RiskResult risk = risks.get(r.sourcePoiId());
            scored.add(new Scored(r, risk, scorer.score(r, risk, condition)));
        }
        scored.sort(Comparator.comparingDouble(Scored::score).reversed());
        List<Scored> topK = scored.subList(0, Math.min(props.getTopK(), scored.size()));

        int poolSize = Math.min(props.getPoolSize(), topK.size());
        // 权重下限 1.0,保证 Top-K 内不会出现零概率(避免"永远第一名")。
        List<Double> weights = topK.stream().map(s -> Math.max(1.0, s.score())).toList();
        List<Scored> picked = new WeightedRandomSelector(System.nanoTime())
                .select(topK, weights, poolSize);

        List<RestaurantCandidate> pool = picked.stream()
                .map(s -> new RestaurantCandidate(
                        s.restaurant(), s.risk(), s.score(),
                        scorer.reasons(s.restaurant(), s.risk(), condition)))
                .toList();
        return new RecommendationResult(pool, props.getAlgorithmVersion());
    }
}
