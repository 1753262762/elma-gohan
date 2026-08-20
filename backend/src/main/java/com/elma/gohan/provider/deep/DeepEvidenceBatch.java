package com.elma.gohan.provider.deep;

import com.elma.gohan.provider.evidence.EvidenceStatus;
import java.time.Instant;
import java.util.List;

public record DeepEvidenceBatch(
        DeepEvidenceSource source,
        EvidenceStatus status,
        List<WebEvidenceItem> items,
        Instant fetchedAt
) {
    public DeepEvidenceBatch {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static DeepEvidenceBatch unavailable(DeepEvidenceSource source, Instant now) {
        return new DeepEvidenceBatch(source, EvidenceStatus.UNAVAILABLE, List.of(), now);
    }
}
