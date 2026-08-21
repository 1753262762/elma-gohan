package com.elma.gohan.domain.recommendation;

import com.elma.gohan.config.RecommendationProperties;
import com.elma.gohan.config.TasteProperties;
import com.elma.gohan.domain.restaurant.DataCompleteness;
import com.elma.gohan.domain.restaurant.Restaurant;
import com.elma.gohan.domain.restaurant.SearchCondition;
import com.elma.gohan.domain.restaurant.TextNormalizer;
import com.elma.gohan.domain.risk.RiskResult;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * LowRegretScore:基础质量 / 距离 / 预算 / 品类 / 数据完整度 / 风险 六因子加权(0~100),
 * 权重全部来自 RecommendationProperties。同时生成面向用户的推荐理由(1~5 条)。
 *
 * v0.3 变更:距离因子改用绝对步行时间饱和函数(与 radius 解耦);
 * 自由文本 dislike 命中改为软降权(dislikePenalty);口味校正按剩余空间比例施加,高分候选不再饱和堆顶。
 */
@Component
public class LowRegretScorer {

    private final RecommendationProperties props;
    private final TasteProperties tasteProperties;

    @Autowired
    public LowRegretScorer(RecommendationProperties props, TasteProperties tasteProperties) {
        this.props = props;
        this.tasteProperties = tasteProperties;
    }

    public LowRegretScorer(RecommendationProperties props) {
        this(props, new TasteProperties());
    }

    public double score(Restaurant r, RiskResult risk, SearchCondition condition) {
        return score(r, risk, new UserPreference(condition));
    }

    public double score(Restaurant r, RiskResult risk, UserPreference preference) {
        SearchCondition condition = preference.condition();
        RecommendationProperties.Weights w = props.getWeights();
        double base = w.getRating() * ratingFactor(r)
                + w.getDistance() * distanceFactor(r, condition)
                + w.getBudget() * budgetFactor(r, condition)
                + w.getCategory() * categoryFactor(r, condition)
                + w.getCompleteness() * completenessFactor(r)
                + w.getRisk() * riskFactor(risk);
        double score = base + dislikePenalty(r, condition) + boundedTasteAdjustment(
                base, tasteAdjustment(r, preference.tasteProfile()));
        return Math.max(0.0, Math.min(100.0, score));
    }

    public List<String> reasons(Restaurant r, RiskResult risk, SearchCondition condition) {
        return reasons(r, risk, new UserPreference(condition));
    }

    public List<String> reasons(Restaurant r, RiskResult risk, UserPreference preference) {
        SearchCondition condition = preference.condition();
        List<String> reasons = new ArrayList<>();
        if (distanceFactor(r, condition) >= 0.6) {
            reasons.add("距离近");
        }
        if (condition.maxBudget() != null && budgetFactor(r, condition) >= 0.8) {
            reasons.add("预算合适");
        }
        if (r.rating() != null && r.rating() >= 4.2) {
            reasons.add("评分稳定");
        }
        if (r.dataCompleteness() == DataCompleteness.FULL) {
            reasons.add("数据完整度较高");
        }
        if (risk.riskLevel() == com.elma.gohan.domain.risk.RiskLevel.LOW) {
            reasons.add("踩坑风险低");
        }
        if (tasteAdjustment(r, preference.tasteProfile()) >= 3.0) {
            reasons.add("符合你的历史口味");
        }
        if (reasons.isEmpty()) {
            reasons.add("综合匹配度较高");
        }
        return reasons.size() > 5 ? reasons.subList(0, 5) : reasons;
    }

    private double ratingFactor(Restaurant r) {
        return r.rating() == null ? 0.3 : Math.max(0, Math.min(1, r.rating() / 5.0));
    }

    /**
     * 绝对步行时间的饱和函数:reference/(reference + 分钟)。
     * 距离为 0 时 1.0,参考分钟数处 0.5,与用户设定的搜索半径无关。
     */
    private double distanceFactor(Restaurant r, SearchCondition c) {
        double referenceMinutes = Math.max(0.1, props.getWalkReferenceMinutes());
        double minutes = r.distanceMeters()
                / (double) Math.max(1, props.getWalkingSpeedMetersPerMinute());
        return referenceMinutes / (referenceMinutes + minutes);
    }

    /**
     * 预算语义:硬上限(超预算已被 HardFilter 剔除)+ 温和的便宜偏好。
     * 不限预算时所有候选同为 0.8,不影响同一次请求内的相对排序。
     */
    private double budgetFactor(Restaurant r, SearchCondition c) {
        if (c.maxBudget() == null) {
            return 0.8;
        }
        if (r.averagePrice() == null) {
            return 0.5;
        }
        if (r.averagePrice() > c.maxBudget()) {
            return 0.0;
        }
        // 预算内越便宜越好:留一半分给"接近预算"的餐厅,避免只推最便宜。
        return 1.0 - 0.5 * r.averagePrice() / c.maxBudget();
    }

    private double categoryFactor(Restaurant r, SearchCondition c) {
        return c.categoryUnlimited() ? 0.9 : 1.0;
    }

    private double completenessFactor(Restaurant r) {
        return switch (r.dataCompleteness()) {
            case FULL -> 1.0;
            case PARTIAL -> 0.6;
            case MINIMAL -> 0.3;
        };
    }

    private double riskFactor(RiskResult risk) {
        double effectiveRisk = risk.confidence() * risk.riskScore()
                + (1.0 - risk.confidence()) * props.getUncertaintyRisk();
        return 1.0 - Math.max(0.0, Math.min(100.0, effectiveRisk)) / 100.0;
    }

    /**
     * 自由文本 dislike 命中名称/品类时的软降权(不剔除):
     * 误杀("面"命中"面对面")可由分数竞争恢复,硬剔除只保留给结构化品类匹配(HardFilter)。
     */
    private double dislikePenalty(Restaurant r, SearchCondition c) {
        if (c.dislikes().isEmpty()) {
            return 0.0;
        }
        String name = TextNormalizer.normalize(r.name());
        String label = TextNormalizer.normalize(r.categoryLabel());
        boolean hit = c.dislikes().stream()
                .map(TextNormalizer::normalize)
                .anyMatch(d -> !d.isEmpty() && (name.contains(d) || label.contains(d)));
        return hit ? -props.getDislikePenalty() : 0.0;
    }

    /**
     * 口味校正按剩余空间比例施加:正向校正乘以剩余分差、负向乘以当前基础分。
     * 基础分接近 100 的候选不再因正向校正集体饱和到 100,保留候选间区分度。
     */
    private double boundedTasteAdjustment(double base, double adjustment) {
        if (adjustment >= 0) {
            return adjustment * (100.0 - base) / 100.0;
        }
        return adjustment * base / 100.0;
    }

    double tasteAdjustment(Restaurant restaurant, TasteProfile profile) {
        if (profile == null || profile.feedbackCount() == 0) return 0.0;
        RecommendationProperties.Taste taste = props.getTaste();
        double maxWeight = Math.max(0.0001, tasteProperties.getMaxAbsoluteWeight());
        double normalized = taste.getCategoryWeight()
                * profile.categoryWeight(restaurant) / maxWeight
                + taste.getPriceWeight()
                * profile.priceWeight(restaurant, tasteProperties) / maxWeight
                + taste.getDistanceWeight()
                * profile.distanceWeight(restaurant, tasteProperties) / maxWeight;
        normalized = Math.max(-1.0, Math.min(1.0, normalized));
        return normalized * taste.getMaxAdjustment();
    }
}
