package com.beetloop.vendorproducts.services.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Lifecycle of a service listing.
 *
 * <p>The wizard's final button is conditional: normally <em>Publish to Catalog</em>,
 * but it becomes <em>Submit for QC</em> when the entry came from the "find" flow
 * and is a custom service (its id is prefixed {@code custom-cm-}, {@code custom-con-},
 * {@code custom-cro-}…). Both routes are modelled here — {@link #PUBLISHED} for the
 * direct path and {@link #SUBMITTED_FOR_QC} for the review path.
 *
 * <p>{@link #getStatusKind()} maps onto the frontend's {@code StatusKind} union so
 * the services table can render a row without translation.
 */
public enum ServiceStatus {

    DRAFT("draft", "Draft"),
    CONFIGURED("draft", "Draft"),
    SUBMITTED_FOR_QC("qc-pending", "QC Pending"),
    PENDING_REVIEW("qc-pending", "Pending Review"),
    QUERY("query", "Query Raised"),
    APPROVED("published", "Approved"),
    REJECTED("query", "Rejected"),
    PUBLISHED("published", "Published");

    private final String statusKind;
    private final String statusLabel;

    ServiceStatus(String statusKind, String statusLabel) {
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

    /** True once the listing has left the vendor's editable draft stage. */
    public boolean isSubmitted() {
        return this != DRAFT && this != CONFIGURED;
    }

    @JsonCreator
    public static ServiceStatus from(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String needle = raw.trim().toUpperCase().replace('-', '_');
        for (ServiceStatus status : values()) {
            if (status.name().equals(needle)) {
                return status;
            }
        }
        String kind = raw.trim().toLowerCase();
        for (ServiceStatus status : values()) {
            if (status.statusKind.equals(kind)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown service status '" + raw + "'");
    }
}
