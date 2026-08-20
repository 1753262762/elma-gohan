package com.elma.gohan.provider.evidence;

import java.time.Instant;

/** 平台无关的聚合证据；任何百度/高德原始结构都必须先映射到这里。 */
public record PlatformEvidence(
        String source,
        String providerPoiId,
        EvidenceStatus status,
        Instant observedAt,
        String name,
        String address,
        Double latitude,
        Double longitude,
        Double overallRating,
        Double tasteRating,
        Double serviceRating,
        Double environmentRating,
        Integer commentCount,
        Integer averagePrice,
        String openingHours,
        String brand,
        String telephone
) {
    public PlatformEvidence {
        status = status == null ? EvidenceStatus.NO_DATA : status;
    }

    public static PlatformEvidence unavailable(String source) {
        return new PlatformEvidence(source, null, EvidenceStatus.UNAVAILABLE, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null);
    }

    public PlatformEvidence mergeOptionalDetails(PlatformEvidence details) {
        if (details == null || details.status() != EvidenceStatus.AVAILABLE) return this;
        return new PlatformEvidence(source, providerPoiId, status,
                later(observedAt, details.observedAt()), name, address, latitude, longitude,
                first(overallRating, details.overallRating()),
                first(tasteRating, details.tasteRating()),
                first(serviceRating, details.serviceRating()),
                first(environmentRating, details.environmentRating()),
                first(commentCount, details.commentCount()),
                first(averagePrice, details.averagePrice()),
                first(openingHours, details.openingHours()), first(brand, details.brand()),
                first(telephone, details.telephone()));
    }

    private static <T> T first(T primary, T fallback) {
        return primary != null ? primary : fallback;
    }

    private static Instant later(Instant a, Instant b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }
}
