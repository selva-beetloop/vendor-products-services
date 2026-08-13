package com.beetloop.catalog.shared.error;

import org.springframework.http.HttpStatus;

/** The Beetloop error catalogue. Mirrors document 09 section 5.2. */
public enum ErrorCode {

    MALFORMED("BL-PS-400-MALFORMED", HttpStatus.BAD_REQUEST, "Malformed request"),
    UNAUTHENTICATED("BL-PS-401-UNAUTHENTICATED", HttpStatus.UNAUTHORIZED, "Not authenticated"),
    FORBIDDEN("BL-PS-403-FORBIDDEN", HttpStatus.FORBIDDEN, "Forbidden"),

    NOT_FOUND("BL-PS-404-NOT-FOUND", HttpStatus.NOT_FOUND, "Not found"),
    UNKNOWN_STEP("BL-PS-404-UNKNOWN-STEP", HttpStatus.NOT_FOUND, "Unknown step"),
    UNKNOWN_SECTION("BL-PS-404-UNKNOWN-SECTION", HttpStatus.NOT_FOUND, "Unknown section"),
    UNKNOWN_TEMPLATE("BL-PS-404-UNKNOWN-TEMPLATE", HttpStatus.NOT_FOUND, "Unknown form template"),

    STALE_VERSION("BL-PS-409-STALE-VERSION", HttpStatus.CONFLICT, "The listing has changed since you loaded it"),
    NOT_EDITABLE("BL-PS-409-NOT-EDITABLE", HttpStatus.CONFLICT, "Not editable"),
    ALREADY_SUBMITTED("BL-PS-409-ALREADY-SUBMITTED", HttpStatus.CONFLICT, "Already submitted"),
    CATEGORY_IMMUTABLE("BL-PS-409-CATEGORY-IMMUTABLE", HttpStatus.CONFLICT, "Category cannot be changed"),
    DOCUMENT_NOT_READY("BL-PS-409-DOCUMENT-NOT-READY", HttpStatus.CONFLICT, "Document not ready"),
    DOCUMENT_IN_USE("BL-PS-409-DOCUMENT-IN-USE", HttpStatus.CONFLICT, "Document in use"),
    CUSTOM_VALUE_IN_USE("BL-PS-409-CUSTOM-VALUE-IN-USE", HttpStatus.CONFLICT, "Custom value in use"),
    ALREADY_CLAIMED("BL-PS-409-ALREADY-CLAIMED", HttpStatus.CONFLICT, "Already claimed"),
    NOT_CLAIMED("BL-PS-409-NOT-CLAIMED", HttpStatus.CONFLICT, "Not claimed"),
    NOT_SUBMITTED("BL-PS-409-NOT-SUBMITTED", HttpStatus.CONFLICT, "Not submitted"),

    FILE_TOO_LARGE("BL-PS-413-FILE-TOO-LARGE", HttpStatus.PAYLOAD_TOO_LARGE, "File too large"),

    VALIDATION("BL-PS-422-VALIDATION", HttpStatus.UNPROCESSABLE_ENTITY, "Validation failed"),
    UNPARSEABLE_DATE("BL-PS-422-UNPARSEABLE-DATE", HttpStatus.UNPROCESSABLE_ENTITY, "Date could not be parsed"),
    DERIVED_FIELD_WRITE("BL-PS-422-DERIVED-FIELD-WRITE", HttpStatus.UNPROCESSABLE_ENTITY, "Derived field cannot be written"),
    COLLECTION_CAP("BL-PS-422-COLLECTION-CAP", HttpStatus.UNPROCESSABLE_ENTITY, "Collection cap exceeded"),
    IDEMPOTENCY_MISMATCH("BL-PS-422-IDEMPOTENCY-MISMATCH", HttpStatus.UNPROCESSABLE_ENTITY,
            "Idempotency key reused with a different payload"),
    FEEDBACK_REQUIRED("BL-PS-422-FEEDBACK-REQUIRED", HttpStatus.UNPROCESSABLE_ENTITY, "Field feedback required"),
    UNSUPPORTED_MIME("BL-PS-422-UNSUPPORTED-MIME", HttpStatus.UNPROCESSABLE_ENTITY, "Unsupported file type"),

    IF_MATCH_REQUIRED("BL-PS-428-IF-MATCH-REQUIRED", HttpStatus.PRECONDITION_REQUIRED, "If-Match is required"),
    RATE_LIMITED("BL-PS-429-RATE-LIMITED", HttpStatus.TOO_MANY_REQUESTS, "Too many requests"),
    INTERNAL("BL-PS-500-INTERNAL", HttpStatus.INTERNAL_SERVER_ERROR, "Internal error");

    private static final String TYPE_BASE = "https://api.beetloop.com/problems/";

    private final String code;
    private final HttpStatus status;
    private final String title;

    ErrorCode(String code, HttpStatus status, String title) {
        this.code = code;
        this.status = status;
        this.title = title;
    }

    public String code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return title;
    }

    /** e.g. BL-PS-409-STALE-VERSION -> https://api.beetloop.com/problems/stale-version */
    public String type() {
        String slug = code.replaceFirst("^BL-PS-\\d{3}-", "").toLowerCase().replace('_', '-');
        return TYPE_BASE + slug;
    }
}
