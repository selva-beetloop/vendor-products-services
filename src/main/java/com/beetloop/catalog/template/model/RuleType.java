package com.beetloop.catalog.template.model;

/**
 * Rules are DATA, not code. Adding a rule type is a code change; adding a USE of an existing rule
 * is a template edit. That is what keeps a new category from touching the controller layer.
 */
public enum RuleType {
    REQUIRED_WHEN,
    MAX_LENGTH,
    RANGE,
    PATTERN,
    IN_VOCABULARY,
    ARRAY_CAP,
    ARRAY_MIN,
    CONTIGUOUS_TIERS,
    AT_LEAST_ONE_OF,
    MUTUALLY_EXCLUSIVE,
    DATE_NOT_IN_PAST,
    DATE_RANGE,
    CROSS_FIELD_COMPARE,
    EXACTLY_ONE_PRIMARY,
    ATTESTATION
}
