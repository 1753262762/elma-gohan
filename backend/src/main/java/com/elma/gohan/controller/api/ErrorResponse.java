package com.elma.gohan.controller.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** 严格对齐 contracts/openapi.yaml 的 ErrorResponse(code/message/可选 fieldErrors/traceId)。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        List<FieldError> fieldErrors,
        String traceId
) {
    public record FieldError(String field, String message) {
    }
}
