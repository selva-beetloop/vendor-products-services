package com.beetloop.catalog.template.model;

/** Every distinct control found across the ten wizards. */
public enum FieldType {
    TEXT,
    TEXTAREA,
    NUMBER,
    SELECT,
    MULTI_SELECT,
    CHIPS,
    CHECKBOX_GROUP,
    RADIO,
    TOGGLE,
    DATE,
    /** Free-text date (the certificate modal) - parsed and normalised server-side. */
    DATE_TEXT,
    /** {value, unit} pair. */
    MEASURE,
    /** {length, width, height, unit}. */
    DIMENSIONS,
    COUNTRY,
    COUNTRY_MULTI,
    FILE_SINGLE,
    FILE_MULTI,
    /** Array of objects: volume pricing tiers, packaging options, layers. */
    REPEATABLE,
    /** Nested object with its own fields. */
    OBJECT,
    /** The two-level specification-group engine. */
    SPEC_GROUPS,
    /** Server-derived text rendered read-only, e.g. Variant Code / SKU. */
    AUTO_TEXT
}
