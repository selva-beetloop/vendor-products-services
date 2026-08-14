package com.beetloop.vendorproducts.exception;

import com.beetloop.vendorproducts.dto.ApiError;

import java.util.ArrayList;
import java.util.List;

/**
 * Thrown when a wizard payload fails the field rules declared in
 * {@code category-schemas.json}. Carries per-field messages in exactly the shape
 * the frontend's inline validation expects.
 */
public class ValidationException extends RuntimeException {

    private final List<ApiError.FieldError> fieldErrors;

    public ValidationException(String message, List<ApiError.FieldError> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public List<ApiError.FieldError> getFieldErrors() {
        return fieldErrors;
    }

    /** Accumulates field errors and throws only if at least one was recorded. */
    public static class Builder {

        private final List<ApiError.FieldError> errors = new ArrayList<>();

        public Builder add(String field, String message) {
            errors.add(new ApiError.FieldError(field, message, null));
            return this;
        }

        public Builder add(String field, String message, Object rejectedValue) {
            errors.add(new ApiError.FieldError(field, message, rejectedValue));
            return this;
        }

        public boolean isEmpty() {
            return errors.isEmpty();
        }

        public void throwIfAny(String summary) {
            if (!errors.isEmpty()) {
                throw new ValidationException(summary, errors);
            }
        }
    }
}
