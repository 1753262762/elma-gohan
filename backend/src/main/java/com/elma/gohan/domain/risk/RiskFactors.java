package com.elma.gohan.domain.risk;

/** risk-v0.3 的五项结构化风险，单项范围均为 0～100。 */
public record RiskFactors(int ratingRisk, int templateRisk, int burstRisk, int trendRisk,
                          int dataInsufficientRisk) {
    public RiskFactors {
        ratingRisk = clamp(ratingRisk);
        templateRisk = clamp(templateRisk);
        burstRisk = clamp(burstRisk);
        trendRisk = clamp(trendRisk);
        dataInsufficientRisk = clamp(dataInsufficientRisk);
    }
    public static RiskFactors empty() { return new RiskFactors(0, 0, 0, 0, 0); }
    private static int clamp(int value) { return Math.max(0, Math.min(100, value)); }
}
