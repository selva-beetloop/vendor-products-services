package com.beetloop.catalog.template;

import com.beetloop.catalog.shared.error.FieldError;
import com.beetloop.catalog.shared.error.RejectedField;
import com.beetloop.catalog.shared.error.Warning;
import com.beetloop.catalog.shared.error.ValidationException;

import java.util.List;
import java.util.Map;

public record ValidationResult(
        List<FieldError> errors,
        List<Warning> warnings,
        List<RejectedField> rejectedFields,
        Map<String, Object> sanitized) {

    public boolean valid() {
        return errors.isEmpty();
    }

    public void throwIfInvalid() {
        if (!valid()) {
            throw new ValidationException(errors, warnings);
        }
    }
}
