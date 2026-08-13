package com.beetloop.catalog.template.model;

import com.beetloop.catalog.shared.model.ListingType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The single design decision that keeps ten wizards on one codebase.
 *
 * Nine Raw Material type cards, seven supply roles, seven Finished Goods roles, an open-ended
 * Packaging Machinery role set behind "More Roles", a Packaging Materials step 2 with no cards at
 * all, variant builders with 5/5/7/6/5 stages, service configurations with 3/5/5/5/11 sub-steps,
 * and an Agro Cluster field whose value switches the schema of the NEXT sub-step.
 *
 * Modelling that as Java DTOs is roughly 80 request classes and a release every time product adds a
 * card. Modelling it as data is one.
 */
@Document(collection = "form_templates")
@CompoundIndex(name = "category_version", def = "{'categoryCode': 1, 'version': -1}")
@CompoundIndex(name = "category_status", def = "{'categoryCode': 1, 'status': 1}")
public record FormTemplate(
        @Id String id,
        ListingType type,
        String categoryCode,
        int version,
        TemplateStatus status,
        String label,
        List<StepSchema> steps,
        ChildCollection childCollection,
        List<SectionSchema> childSections,
        List<GridColumn> gridColumns,
        List<TabSchema> variantTabs,
        Map<String, Object> seedData,
        Instant publishedAt) {

    public Optional<StepSchema> step(String keyOrDataKey) {
        if (steps == null || keyOrDataKey == null) {
            return Optional.empty();
        }
        return steps.stream()
                .filter(s -> keyOrDataKey.equals(s.key()) || keyOrDataKey.equals(s.dataKey()))
                .findFirst();
    }

    public List<String> stepKeys() {
        return steps == null ? List.of() : steps.stream().map(StepSchema::key).toList();
    }

    public Optional<SectionSchema> childSection(String keyOrDataKey) {
        if (childSections == null || keyOrDataKey == null) {
            return Optional.empty();
        }
        return childSections.stream()
                .filter(s -> keyOrDataKey.equals(s.key()) || keyOrDataKey.equals(s.dataKey()))
                .findFirst();
    }

    public List<String> childSectionKeys() {
        return childSections == null ? List.of() : childSections.stream().map(SectionSchema::key).toList();
    }

    // ---------------------------------------------------------------- nested schema

    /**
     * @param key     kebab-case, appears in the URL: /steps/{key}
     * @param dataKey camelCase, the key inside data{}
     */
    public record StepSchema(
            String key,
            String dataKey,
            String label,
            String pageHeading,
            String subtitle,
            boolean required,
            String primaryAction,
            Discriminator discriminator,
            List<SectionSchema> sections) {
    }

    /**
     * The card selector that replaces the whole form below it: Raw Material Type (9 cards),
     * Your Role (7 cards), Agro Cluster infrastructure provider type.
     */
    public record Discriminator(
            String field,
            String label,
            String control,
            String defaultValue,
            String helpText,
            List<DiscriminatorOption> options) {

        public Optional<DiscriminatorOption> option(String code) {
            if (options == null || code == null) {
                return Optional.empty();
            }
            return options.stream().filter(o -> code.equals(o.code())).findFirst();
        }

        public List<String> codes() {
            return options == null ? List.of() : options.stream().map(DiscriminatorOption::code).toList();
        }
    }

    /**
     * @param storeUnder the data{} key the sub-form lives under. Storing each card under its own key
     *                   is why switching cards and switching back does not destroy the first card's
     *                   data - which the UI lets a vendor do freely.
     * @param primary    false for the roles hidden behind the "More Roles" overflow card.
     */
    public record DiscriminatorOption(
            String code,
            String label,
            String storeUnder,
            String cardCopy,
            Boolean primary,
            List<SectionSchema> sections) {
    }

    /** A section with a blank key renders its fields at the parent level rather than nesting. */
    public record SectionSchema(
            String key,
            String dataKey,
            String label,
            String subtitle,
            Integer order,
            boolean repeatable,
            List<FieldSchema> fields,
            List<SectionSchema> sections,
            List<TabSchema> tabs) {
    }

    public record FieldSchema(
            String key,
            String label,
            FieldType type,
            String placeholder,
            String help,
            boolean required,
            Integer maxLength,
            Double min,
            Double max,
            Integer maxItems,
            Integer minItems,
            String vocabularyCode,
            String parentField,
            String unitVocabularyCode,
            List<String> units,
            FieldAccess access,
            boolean allowCustomValues,
            String pattern,
            List<FieldSchema> fields,
            List<RuleSpec> rules) {

        public FieldAccess accessOrDefault() {
            return access == null ? FieldAccess.EDITABLE : access;
        }

        public boolean writable() {
            return accessOrDefault() == FieldAccess.EDITABLE;
        }
    }

    public record RuleSpec(RuleType type, Map<String, Object> params, String message) {
    }

    public record GridColumn(String key, String label) {
    }

    public record TabSchema(String key, String label) {
    }
}
