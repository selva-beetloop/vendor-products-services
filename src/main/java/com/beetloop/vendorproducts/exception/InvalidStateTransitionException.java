package com.beetloop.vendorproducts.exception;

import com.beetloop.vendorproducts.domain.ProductStatus;

/** 409 — the requested action is not allowed from the product's current status. */
public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(String message) {
        super(message);
    }

    public static InvalidStateTransitionException notEditable(ProductStatus status) {
        return new InvalidStateTransitionException(
                "Product is " + status.getStatusLabel() + " and can no longer be edited. "
                        + "Only DRAFT products (or a product returned by a QC query/rejection) accept edits.");
    }

    public static InvalidStateTransitionException cannotSubmit(ProductStatus status) {
        return new InvalidStateTransitionException(
                "Product cannot be submitted for QC from status " + status.name());
    }

    public static InvalidStateTransitionException cannotReview(ProductStatus status) {
        return new InvalidStateTransitionException(
                "Product is not awaiting QC review (current status " + status.name() + ")");
    }
}
