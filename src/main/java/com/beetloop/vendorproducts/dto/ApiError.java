package com.beetloop.vendorproducts.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Consistent error envelope for every failure.
 *
 * <p>{@link #fieldErrors} is keyed by the same field name the wizard uses in its
 * form state, so the frontend can drop the map straight into its existing
 * {@code FormErrors} state and render inline messages without translation.
 */
@Schema(description = "Error response. `fieldErrors` maps form field name → message.")
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> errors,
        Map<String, String> fieldErrors) {

    public record FieldError(String field, String message, Object rejectedValue) {
    }

    public static ApiError of(int status, String error, String message, String path, List<FieldError> errors) {
        Map<String, String> asMap = errors == null ? Map.of() : errors.stream()
                .collect(Collectors.toMap(
                        FieldError::field,
                        FieldError::message,
                        (first, second) -> first,
                        java.util.LinkedHashMap::new));
        return new ApiError(Instant.now(), status, error, message, path,
                errors == null ? List.of() : errors, asMap);
    }
}
