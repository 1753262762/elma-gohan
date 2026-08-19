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
 * V0.1 默认推荐引擎:
 * 硬过滤 -> 高风险剔除 -> LowRegretScore 排序 -> Top-K -> 加权随机抽取候选池(A/B/C)。
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
                                          UserPreference preference) {
        var condition = preference.condition();
        List<Restaurant> filtered = hardFilter.filter(candidates, condition).stream()
                .filter(r -> !isBlocked(risks.get(r.sourcePoiId())))
                .toList();
        if (filtered.isEmpty()) {
            return new RecommendationResult(List.of(), props.getAlgorithmVersion());
        }

        List<Scored> scored = new ArrayList<>();
        for (Restaurant r : filtered) {
            RiskResult risk = risks.get(r.sourcePoiId());
            scored.add(new Scored(r, risk, scorer.score(r, risk, condition)));
        }
        scored.sort(Comparator.comparingDouble(Scored::score).reversed());
        List<Scored> diversified = diversify(scored, condition);
        List<Scored> topK = diversified.subList(0, Math.min(props.getTopK(), diversified.size()));

        int poolSize = Math.min(props.getPoolSize(), topK.size());
        List<Scored> shortlist = topK.subList(0, poolSize);
        // 权重下限 1.0,保证 Top-K 内不会出现零概率(避免"永远第一名")。
        List<Double> weights = shortlist.stream().map(s -> Math.max(1.0, s.score())).toList();
        List<Scored> picked = new WeightedRandomSelector(System.nanoTime())
                .select(shortlist, weights, poolSize);
        // 随机决定均衡候选集的展示起点后,再次交错品类,避免连续出现同类店。
        picked = diversify(picked, condition);

        List<RestaurantCandidate> pool = picked.stream()
                .map(s -> new RestaurantCandidate(
                        s.restaurant(), s.risk(), s.score(),
                        scorer.reasons(s.restaurant(), s.risk(), condition)))
                .toList();
        return new RecommendationResult(pool, props.getAlgorithmVersion());
    }

    /**
     * 按品类轮询重排:ANY 在产品大类间均衡;明确大类时在其内部细品类间均衡。
     * 某一组候选不足时自然回填其他组,不会为了多样性丢掉可用候选。
     */
    private List<Scored> diversify(List<Scored> ranked, SearchCondition condition) {
        Map<String, List<Scored>> groups = new LinkedHashMap<>();
        for (Scored item : ranked) {
            String key = diversityKey(item.restaurant(), condition);
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(item);
        }

        List<Scored> result = new ArrayList<>(ranked.size());
        for (int depth = 0; result.size() < ranked.size(); depth++) {
            for (List<Scored> group : groups.values()) {
                if (depth < group.size()) {
                    result.add(group.get(depth));
                }
            }
        }
        return result;
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
