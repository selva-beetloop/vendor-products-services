package com.beetloop.catalog.shared.error;

/**
 * A write to a read-only field does not fail the request. It is reported here and dropped.
 * A byte-identical echo of a LINKED/DERIVED value is dropped SILENTLY (no entry here), because the
 * client legitimately round-trips the whole object it was given. Only a changed value is reported.
 */
public record RejectedField(String path, String reason, String message, Object rejectedValue) {

    public static final String READ_ONLY_AUTO = "READ_ONLY_AUTO";
    public static final String READ_ONLY_LINKED = "READ_ONLY_LINKED";
    public static final String DERIVED_FIELD = "DERIVED_FIELD";
    public static final String SYSTEM_FIELD = "SYSTEM_FIELD";
    public static final String BILLING_GATED_FIELD = "BILLING_GATED_FIELD";
    public static final String NOT_ELIGIBLE = "NOT_ELIGIBLE";

    public static RejectedField of(String path, String reason, String message) {
        return new RejectedField(path, reason, message, null);
    }
}
