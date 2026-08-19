package com.elma.gohan.controller.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/** 严格对齐 contracts/openapi.yaml 的 RestaurantSummary;category 内嵌 code/label。 */
public record RestaurantSummary(
        String id,
        String name,
        double latitude,
        double longitude,
        String address,
        Category category,
        int distanceMeters,
        int walkingMinutes,
        @JsonInclude(JsonInclude.Include.ALWAYS) Integer averagePrice,
        @JsonInclude(JsonInclude.Include.ALWAYS) Double rating,
        String businessStatus
) {
    public record Category(String code, String label) {
    }
}
