package com.beetloop.vendorproducts.catalogue.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CatalogueKind {
    PRODUCT,
    SERVICE;

    @JsonValue
    public String getName() {
        return name();
    }

    @JsonCreator
    public static CatalogueKind from(String raw) {
        if (raw == null || raw.isBlank()) {
            return PRODUCT;
        }
        return valueOf(raw.trim().toUpperCase());
    }
}
