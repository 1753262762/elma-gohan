package com.elma.gohan.controller.api;

import java.util.List;

/** 严格对齐 contracts/openapi.yaml 的 RiskAssessment。 */
public record RiskAssessment(
        int riskScore,
        String riskLevel,
        double confidence,
        List<String> reasons,
        String algorithmVersion
) {
}
