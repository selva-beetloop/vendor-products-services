package com.beetloop.vendorproducts.exception;

import java.util.UUID;

/** 409 — this vendor already has a non-rejected T3 for the same T2. */
public class DuplicateListingException extends InvalidStateTransitionException {

    private final UUID existingId;
    private final String listingCode;

    public DuplicateListingException(UUID existingId, String listingCode) {
        super("Duplicate listing for this commercial master. Existing T3: "
                + existingId + (listingCode == null ? "" : " (" + listingCode + ")"));
        this.existingId = existingId;
        this.listingCode = listingCode;
    }

    public UUID getExistingId() {
        return existingId;
    }

    public String getListingCode() {
        return listingCode;
    }
}
