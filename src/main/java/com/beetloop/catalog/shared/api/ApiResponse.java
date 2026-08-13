package com.beetloop.catalog.shared.api;

/** Every successful single-resource response is wrapped as {"data": ...}. */
public record ApiResponse<T>(T data) {
    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data);
    }
}
