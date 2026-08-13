package com.beetloop.catalog.product.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Embedded in the listing. Sections are keyed by the category's variant stages - 5 for Raw
 * Materials and the two machinery categories, 7 for Finished Goods, 6 for Packaging Materials.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {

    private String variantId;

    @Builder.Default
    private Map<String, Object> sections = new LinkedHashMap<>();

    @Builder.Default
    private String status = "DRAFT";

    /** DERIVED. */
    private int completionPercent;

    private Instant createdAt;
    private Instant updatedAt;

    public Map<String, Object> section(String dataKey) {
        Object value = sections == null ? null : sections.get(dataKey);
        return value instanceof Map<?, ?> ? castMap(value) : null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
