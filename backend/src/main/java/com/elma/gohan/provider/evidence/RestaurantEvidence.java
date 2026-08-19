package com.elma.gohan.provider.evidence;

import java.time.Instant;
import java.util.List;

/** 与具体评论平台无关的餐厅证据。poolAveragePrice 由编排层补充。 */
public record RestaurantEvidence(
        String source,
        EvidenceStatus status,
        List<ReviewEvidence> reviews,
        Instant fetchedAt,
        Double poolAveragePrice
) {
    public RestaurantEvidence {
        reviews = reviews == null ? List.of() : List.copyOf(reviews);
        status = status == null ? EvidenceStatus.NO_DATA : status;
    }

    public static RestaurantEvidence empty() {
        return noData("EMPTY");
    }

    public static RestaurantEvidence noData(String source) {
        return new RestaurantEvidence(source, EvidenceStatus.NO_DATA, List.of(), null, null);
    }

    public static RestaurantEvidence unavailable(String source) {
        return new RestaurantEvidence(source, EvidenceStatus.UNAVAILABLE, List.of(), null, null);
    }

    public static RestaurantEvidence available(String source, List<ReviewEvidence> reviews,
                                               Instant fetchedAt) {
        if (reviews == null || reviews.isEmpty()) {
            return noData(source);
        }
        return new RestaurantEvidence(source, EvidenceStatus.AVAILABLE, reviews, fetchedAt, null);
    }

    public RestaurantEvidence withPoolAveragePrice(double avg) {
        return new RestaurantEvidence(source, status, reviews, fetchedAt, avg);
    }
}
