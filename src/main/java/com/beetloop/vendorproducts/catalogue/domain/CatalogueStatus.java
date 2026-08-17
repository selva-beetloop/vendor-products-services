package com.beetloop.vendorproducts.catalogue.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Intelligence-owned lifecycle for T1 / T2. LIVE is the only status Flow A may search.
 */
public enum CatalogueStatus {
    DRAFT,
    SUBMITTED,
    IN_QC,
    QUERY,
    APPROVED,
    LIVE,
    REJECTED,
    SUSPENDED,
    PENDING_SCIENTIFIC_MASTER;

    @JsonValue
    public String getName() {
        return name();
    }

    public boolean isLive() {
        return this == LIVE || this == APPROVED;
    }

    public boolean isInIntelQueue() {
        return this == SUBMITTED || this == IN_QC;
    }

    @JsonCreator
    public static CatalogueStatus from(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return valueOf(raw.trim().toUpperCase().replace('-', '_'));
    }
}
