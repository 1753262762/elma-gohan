package com.elma.gohan.domain.risk;

public record BurstDetectionResult(int burstRisk, int peakCount, double peakBaselineRatio) {
    public static BurstDetectionResult none() { return new BurstDetectionResult(0, 0, 0.0); }
}
