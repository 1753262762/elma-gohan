package com.elma.gohan.domain.risk;

import java.util.List;

public record RiskResult(
        int riskScore,
        RiskLevel riskLevel,
        double confidence,
        RiskFactors factors,
        List<String> reasons,
        String algorithmVersion
) {
    public RiskResult {
        riskScore = Math.max(0, Math.min(100, riskScore));
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        factors = factors == null ? RiskFactors.empty() : factors;
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }

    public RiskResult(int riskScore, RiskLevel riskLevel, List<String> reasons,
                      String algorithmVersion) {
        this(riskScore, riskLevel, 1.0, RiskFactors.empty(), reasons, algorithmVersion);
    }
}
