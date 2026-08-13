package com.beetloop.catalog.shared.error;

import java.util.Map;

/**
 * `path` is a JSON-pointer-style path into the REQUEST BODY so the client can map an error to a
 * control without a lookup table. `label` is the UI label from the form template so the message
 * reads the way the field is labelled on screen.
 */
public record FieldError(
        String step,
        String path,
        String label,
        String code,
        String message,
        Object rejectedValue,
        String condition,
        Map<String, Object> meta) {

    public static FieldError of(String step, String path, String label, String code, String message) {
        return new FieldError(step, path, label, code, message, null, null, null);
    }

    public static FieldError of(String step, String path, String label, String code, String message,
                                Object rejectedValue) {
        return new FieldError(step, path, label, code, message, rejectedValue, null, null);
    }

    public FieldError withMeta(Map<String, Object> extra) {
        return new FieldError(step, path, label, code, message, rejectedValue, condition, extra);
    }

    public FieldError withCondition(String cond) {
        return new FieldError(step, path, label, code, message, rejectedValue, cond, meta);
    }
}
