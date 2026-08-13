package com.beetloop.catalog.product.model;

import java.util.List;

/**
 * All five product categories share four outer steps; only the LABELS differ
 * (Material Identification / Machine Identity / Product Identity, and Review & Submit vs
 * Review & Publish). Labels come from the form template - these are the canonical keys.
 */
public final class ProductStepKey {

    public static final String IDENTITY = "identity";
    public static final String ROLE = "role";
    public static final String VARIANTS = "variants";
    public static final String REVIEW = "review";

    public static final List<String> ALL = List.of(IDENTITY, ROLE, VARIANTS, REVIEW);

    /** The review step is an acknowledgement only; it never counts towards completeness. */
    public static final List<String> COMPLETABLE = List.of(IDENTITY, ROLE, VARIANTS);

    private ProductStepKey() {
    }
}
