package com.elma.gohan.domain.recommendation;

import com.elma.gohan.domain.restaurant.Restaurant;
import com.elma.gohan.domain.risk.RiskLevel;
import com.elma.gohan.domain.risk.RiskResult;
import java.util.List;
import java.util.Map;

/**
 * 推荐引擎抽象:输入候选(已含风险结果),输出有序候选池。
 */
public interface RecommendationEngine {

    RecommendationResult recommend(List<Restaurant> candidates,
                                   Map<String, RiskResult> risks,
                                   UserPreference preference);

    /** 高风险(61+)不主动推荐。 */
    default boolean isBlocked(RiskResult risk) {
        return risk.riskLevel() == RiskLevel.HIGH;
    }
}
