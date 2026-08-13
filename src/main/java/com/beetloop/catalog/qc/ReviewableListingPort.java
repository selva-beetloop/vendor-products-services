package com.beetloop.catalog.qc;

import com.beetloop.catalog.shared.model.Lifecycle;
import com.beetloop.catalog.shared.model.QcStatus;

import java.util.Optional;

/**
 * Implemented once per listing type. Keeps the QC module from depending on the product and service
 * modules directly, and keeps approve/reject identical for both.
 */
public interface ReviewableListingPort {

    /** PRODUCT_LISTING or SERVICE_LISTING. */
    String entityType();

    Optional<Snapshot> find(String entityId);

    void applyDecision(String entityId, QcStatus qcStatus, Lifecycle lifecycle);

    record Snapshot(String id, String code, String vendorId, String categoryCode, String name,
                    Object body, long version) {
    }
}
