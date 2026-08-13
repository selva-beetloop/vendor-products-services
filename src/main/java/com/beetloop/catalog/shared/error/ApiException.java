package com.beetloop.catalog.shared.error;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient Map<String, Object> extensions = new LinkedHashMap<>();

    public ApiException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }

    public ApiException with(String key, Object value) {
        if (value != null) {
            extensions.put(key, value);
        }
        return this;
    }

    public static ApiException notFound(String what) {
        return new ApiException(ErrorCode.NOT_FOUND, what + " was not found.");
    }

    public static ApiException of(ErrorCode code, String detail) {
        return new ApiException(code, detail);
    }
}
