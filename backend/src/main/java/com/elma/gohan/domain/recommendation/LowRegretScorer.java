package com.elma.gohan.domain.recommendation;

import com.elma.gohan.config.RecommendationProperties;
import com.elma.gohan.domain.restaurant.DataCompleteness;
import com.elma.gohan.domain.restaurant.Restaurant;
import com.elma.gohan.domain.restaurant.SearchCondition;
import com.elma.gohan.domain.risk.RiskResult;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * LowRegretScore:基础质量 / 距离 / 预算 / 品类 / 数据完整度 / 风险 六因子加权(0~100),
 * 权重全部来自 RecommendationProperties。同时生成面向用户的推荐理由(1~5 条)。
 */
@Component
public class LowRegretScorer {

    private final RecommendationProperties props;

    public LowRegretScorer(RecommendationProperties props) {
        this.props = props;
    }

    public double score(Restaurant r, RiskResult risk, SearchCondition condition) {
        RecommendationProperties.Weights w = props.getWeights();
        return w.getRating() * ratingFactor(r)
                + w.getDistance() * distanceFactor(r, condition)
                + w.getBudget() * budgetFactor(r, condition)
                + w.getCategory() * categoryFactor(r, condition)
                + w.getCompleteness() * completenessFactor(r)
                + w.getRisk() * riskFactor(risk);
    }

    public List<String> reasons(Restaurant r, RiskResult risk, SearchCondition condition) {
        List<String> reasons = new ArrayList<>();
        if (distanceFactor(r, condition) >= 0.6) {
            reasons.add("距离近");
        }
        if (budgetFactor(r, condition) >= 0.8) {
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
        if (reasons.isEmpty()) {
            reasons.add("综合匹配度较高");
        }
        return reasons.size() > 5 ? reasons.subList(0, 5) : reasons;
    }

    private double ratingFactor(Restaurant r) {
        return r.rating() == null ? 0.3 : Math.max(0, Math.min(1, r.rating() / 5.0));
    }

    private double distanceFactor(Restaurant r, SearchCondition c) {
        if (c.radius() <= 0) {
            return 0.5;
        }
        return Math.max(0, Math.min(1, 1.0 - (double) r.distanceMeters() / c.radius()));
    }

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
        return 1.0 - risk.riskScore() / 100.0;
    }
}
