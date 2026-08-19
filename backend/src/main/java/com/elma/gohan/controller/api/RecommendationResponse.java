package com.elma.gohan.controller.api;

import java.util.List;

/** 严格对齐 contracts/openapi.yaml 的 RecommendationResponse。 */
public record RecommendationResponse(
        String recommendationId,
        RestaurantSummary restaurant,
        RiskAssessment risk,
        List<String> reasons,
        int alternativesRemaining
) {
}
