package com.beetloop.vendorproducts.services.service;

/**
 * Client-generated ids for vendor-created (non-catalogue) services.
 *
 * <p>Lab Testing / Agro use {@code custom-…}, Consultancy {@code custom-con-…},
 * Contract Manufacturer {@code custom-cm-…}, and CRO {@code cro-custom-…}. A
 * prefix check of only {@code custom-} therefore misses CRO and incorrectly
 * treats a custom study as a catalogue lookup.
 */
public final class CustomSourceIds {

    private CustomSourceIds() {
    }

    public static boolean isCustom(String sourceServiceId) {
        if (sourceServiceId == null || sourceServiceId.isBlank()) {
            return false;
        }
        return sourceServiceId.startsWith("custom-")
                || sourceServiceId.startsWith("cro-custom-")
                || "cro-new-service".equals(sourceServiceId);
    }
}
