package com.beetloop.vendorproducts.registry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * Declarative schema objects backing {@code category-schemas.json}. Together they
 * form the per-category, per-type-card, per-role-card field inventory that both
 * validates incoming payloads and is published by {@code GET /catalog/categories}.
 */
public final class CategorySchema {

    private CategorySchema() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Root(Map<String, Category> categories, Map<String, Object> sharedVariantSections) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Category(
            String label,
            String groupId,
            String actionButtonLabel,
            String typeLabel,
            List<Step> steps,
            Identity identity,
            Map<String, Section> roles,
            List<FieldDefinition> roleSharedFields,
            Variant variant) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Step(String key, String title, String subtitle) {
    }

    /**
     * Step 1. {@code baseFields} always applies; {@code types} carries the
     * per-card field sets when the category has a type selector (Raw Materials).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Identity(
            String typeSelectorLabel,
            List<FieldDefinition> baseFields,
            Map<String, Section> types) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Section(String title, String description, List<FieldDefinition> fields) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Variant(
            List<Step> subSteps,
            List<FieldDefinition> detailFields,
            List<FieldDefinition> extraFields) {
    }
}
