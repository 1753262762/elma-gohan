package com.elma.gohan.controller.api;

/** 严格对齐 contracts/openapi.yaml 的 FeedbackResponse。 */
public record FeedbackResponse(
        String feedbackId,
        String recommendationId,
        String restaurantId,
        String result,
        String recordedAt
) {
}
