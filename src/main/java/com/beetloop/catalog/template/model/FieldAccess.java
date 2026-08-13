package com.beetloop.catalog.template.model;

public enum FieldAccess {
    EDITABLE,
    /** Server-generated: variantCodeSku, requestCode, code. */
    AUTO,
    /** Mirrored from a master or facility record: facilitySnapshot, masterProduct. */
    LINKED,
    /** vendorId, qcStatus, lifecycle, version, timestamps. */
    SYSTEM,
    /** Computed: counters, overallCompletion, certificate status, analysisSummary. */
    DERIVED,
    /** Paid promotion tiers - activated through the promotion endpoint, not a save. */
    BILLING_GATED
}
