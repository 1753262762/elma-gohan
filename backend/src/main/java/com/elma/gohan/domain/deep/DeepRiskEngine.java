package com.elma.gohan.domain.deep;

public interface DeepRiskEngine {
    DeepRiskResult evaluate(BaseRiskSnapshot baseRisk, DeepSignalAnalysis analysis);
}
