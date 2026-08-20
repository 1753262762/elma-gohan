package com.elma.gohan.domain.deep;

import com.elma.gohan.domain.risk.RiskLevel;
import java.util.List;

public record BaseRiskSnapshot(
        int riskScore,
        RiskLevel riskLevel,
        double confidence,
        List<String> reasons,
        String algorithmVersion
) {
    public BaseRiskSnapshot {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}
