package com.beetloop.catalog.servicelisting.model;

public enum ServiceCategoryCode {

    LAB_TESTING("LAB"),
    CONSULTANCY("CON"),
    CONTRACT_MANUFACTURER("CM"),
    AGRO_CLUSTER("AGR"),
    CRO("CRO"),
    OTHERS_SERVICE("OTH");

    private final String abbreviation;

    ServiceCategoryCode(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String abbreviation() {
        return abbreviation;
    }
}
