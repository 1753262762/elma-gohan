package com.elma.gohan.application;

import java.util.LinkedHashMap;
import java.util.Map;

/** 字段校验失败(radius 枚举、请求头 UUID 等) -> 400 VALIDATION_FAILED + fieldErrors。 */
public class ValidationFailedException extends RuntimeException {

    private final Map<String, String> fieldErrors = new LinkedHashMap<>();

    public ValidationFailedException(String field, String message) {
        super(message);
        this.fieldErrors.put(field, message);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
