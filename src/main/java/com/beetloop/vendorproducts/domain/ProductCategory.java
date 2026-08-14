package com.beetloop.vendorproducts.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The five product categories offered on the "List a Product" screen
 * ({@code AddProductListingPage.tsx}). The wire value is the kebab-case id the
 * frontend already uses as {@code CategoryId}; {@link #getGroupId()} is the
 * {@code ProductGroupId} used by the catalog listing chips.
 */
public enum ProductCategory {

    RAW_MATERIALS("raw-materials", "materials", "Material", "Add New Material"),
    PROCESSING_MACHINERY("processing-machinery", "processing-machinery", "Processing Machinery", "Add New Machine"),
    FINISHED_GOODS("finished-goods", "finished", "Finished Good", "Add New Product"),
    PACKAGING_MATERIALS("packaging-materials", "packaging-materials", "Packaging Material", "Add New Packaging Material"),
    PACKAGING_MACHINERY("packaging-machinery", "packaging-machinery", "Packaging Machinery", "Add New Machine");

    private final String id;
    private final String groupId;
    private final String typeLabel;
    private final String actionButtonLabel;

    ProductCategory(String id, String groupId, String typeLabel, String actionButtonLabel) {
        this.id = id;
        this.groupId = groupId;
        this.typeLabel = typeLabel;
        this.actionButtonLabel = actionButtonLabel;
    }

    @JsonValue
    public String getId() {
        return id;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getTypeLabel() {
        return typeLabel;
    }

    public String getActionButtonLabel() {
        return actionButtonLabel;
    }

    /** Accepts the kebab-case id, the enum name, or the catalog groupId. */
    @JsonCreator
    public static ProductCategory from(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String needle = raw.trim().toLowerCase().replace('_', '-');
        for (ProductCategory category : values()) {
            if (category.id.equals(needle)
                    || category.groupId.equals(needle)
                    || category.name().toLowerCase().replace('_', '-').equals(needle)) {
                return category;
            }
        }
        throw new IllegalArgumentException(
                "Unknown product category '" + raw + "'. Expected one of: raw-materials, processing-machinery, "
                        + "finished-goods, packaging-materials, packaging-machinery");
    }
}
