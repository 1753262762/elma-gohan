package com.elma.gohan.provider.evidence;

import java.util.List;

public record PlatformSearchResult(EvidenceStatus status, List<PlatformEvidence> evidence) {
    public PlatformSearchResult {
        status = status == null ? EvidenceStatus.UNAVAILABLE : status;
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    public static PlatformSearchResult unavailable() {
        return new PlatformSearchResult(EvidenceStatus.UNAVAILABLE, List.of());
    }
}
