package com.beetloop.catalog.product.model;

public enum ProductCategoryCode {

    RAW_MATERIALS("RM"),
    PROCESSING_MACHINERY("PRM"),
    FINISHED_GOODS("FG"),
    PACKAGING_MATERIALS("PKM"),
    PACKAGING_MACHINERY("PKX"),
    OTHERS_PRODUCT("OTH");

    private final String abbreviation;

    ProductCategoryCode(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    /** Used in the Path B request code, e.g. REQ-RM-2026-0042. */
    public String abbreviation() {
        return abbreviation;
    }
}
