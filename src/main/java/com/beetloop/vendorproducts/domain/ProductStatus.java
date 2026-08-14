package com.beetloop.vendorproducts.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Wizard + QC state machine for a vendor product.
 *
 * <pre>
 * DRAFT → IDENTITY_SAVED → ROLE_SAVED → VARIANTS_SAVED → SUBMITTED_FOR_QC
 *                                                              ↓
 *                                                       PENDING_REVIEW
 *                                                        ↓     ↓     ↓
 *                                                 APPROVED  REJECTED  QUERY
 *                                                     ↓
 *                                                 PUBLISHED
 * </pre>
 *
 * {@link #getStatusKind()} / {@link #getStatusLabel()} map onto the frontend's
 * {@code StatusKind} union and the badge text used in {@code catalog-data.ts}.
 */
public enum ProductStatus {

    DRAFT("draft", "Draft"),
    IDENTITY_SAVED("draft", "Draft"),
    ROLE_SAVED("draft", "Draft"),
    VARIANTS_SAVED("draft", "Draft"),
    SUBMITTED_FOR_QC("qc-pending", "QC Pending"),
    PENDING_REVIEW("qc-pending", "Pending Review"),
    QUERY("query", "Query Raised"),
    APPROVED("published", "Approved"),
    REJECTED("query", "Rejected"),
    PUBLISHED("published", "Published");

    private final String statusKind;
    private final String statusLabel;

    ProductStatus(String statusKind, String statusLabel) {
        this.statusKind = statusKind;
        this.statusLabel = statusLabel;
    }

    @JsonValue
    public String getName() {
        return name();
    }

    public String getStatusKind() {
        return statusKind;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    /** True once the product has left the vendor's editable draft stage. */
    public boolean isSubmitted() {
        return this == SUBMITTED_FOR_QC || this == PENDING_REVIEW || this == APPROVED
                || this == REJECTED || this == PUBLISHED || this == QUERY;
    }

    @JsonCreator
    public static ProductStatus from(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String needle = raw.trim().toUpperCase().replace('-', '_');
        for (ProductStatus status : values()) {
            if (status.name().equals(needle)) {
                return status;
            }
        }
        // also accept the frontend's StatusKind values, e.g. "qc-pending"
        String kind = raw.trim().toLowerCase();
        for (ProductStatus status : values()) {
            if (status.statusKind.equals(kind)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown product status '" + raw + "'");
    }
}
