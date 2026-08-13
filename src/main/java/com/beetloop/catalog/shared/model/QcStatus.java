package com.beetloop.catalog.shared.model;

/**
 * There is deliberately no DRAFT value. A listing that has never been submitted has
 * qcStatus == null and renders as "Draft" in the UI.
 */
public enum QcStatus {
    PENDING_REVIEW,
    IN_REVIEW,
    APPROVED,
    REJECTED
}
