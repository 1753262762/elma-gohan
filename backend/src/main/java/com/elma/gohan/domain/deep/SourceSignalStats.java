package com.elma.gohan.domain.deep;

public record SourceSignalStats(
        int relevantCount,
        int positiveCount,
        int negativeCount,
        int operationalCount,
        int marketingCount,
        double balance,
        String direction
) {
}
