package com.elma.gohan.domain.risk;

import java.util.List;

public record RiskResult(
        int riskScore,
        RiskLevel riskLevel,
        List<String> reasons,
        String algorithmVersion
) {
}
