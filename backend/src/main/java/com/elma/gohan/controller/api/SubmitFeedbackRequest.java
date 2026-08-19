package com.elma.gohan.controller.api;

import jakarta.validation.constraints.NotNull;

/** 严格对齐 contracts/openapi.yaml 的 SubmitFeedbackRequest(只有 result)。 */
public record SubmitFeedbackRequest(
        @NotNull(message = "必填") Result result
) {
    public enum Result { LIKE, NORMAL, DISLIKE }
}
