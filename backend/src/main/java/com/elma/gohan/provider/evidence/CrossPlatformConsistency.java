package com.elma.gohan.provider.evidence;

public record CrossPlatformConsistency(
        ConsistencyLevel level,
        Double ratingDifference,
        int crossPlatformConflictRisk,
        String reason
) {
    public CrossPlatformConsistency {
        level = level == null ? ConsistencyLevel.UNKNOWN : level;
        crossPlatformConflictRisk = Math.max(0, Math.min(100, crossPlatformConflictRisk));
        reason = reason == null ? "跨平台一致性暂不可判断" : reason;
    }

    public static CrossPlatformConsistency unknown(String reason) {
        return new CrossPlatformConsistency(ConsistencyLevel.UNKNOWN, null, 0, reason);
    }
}
