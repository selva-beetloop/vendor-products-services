package com.beetloop.vendorproducts.services.registry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/** Shape of {@code service-schemas.json}. */
public final class ServiceSchema {

    private ServiceSchema() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Root(Map<String, Category> categories) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Category(
            String label,
            String actionButtonLabel,
            int stageCount,
            boolean multiServiceSupported,
            List<Stage> stages,
            Accreditations accreditations,
            int fieldCount) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Stage(String key, String title, List<Section> sections) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Section(String heading, boolean repeatable, List<Field> fields) {
    }

    /**
     * One control on a service wizard screen.
     *
     * @param control text | textarea | number | currency | date | select |
     *                multiselect | checkbox | radio | toggle | tags | file |
     *                image | repeatable | readonly
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Field(
            String name,
            String label,
            String control,
            boolean required,
            List<String> options,
            String defaultValue,
            String validation,
            String dependsOn,
            @com.fasterxml.jackson.annotation.JsonProperty("rootKey")
            String rootKey) {

        /**
         * Top-level payload key this field belongs to. A declared nested path such
         * as {@code supportingAssets[].title} has rootKey {@code supportingAssets},
         * so a client sending the parent object satisfies it.
         */
        public String resolveRootKey() {
            if (rootKey != null && !rootKey.isBlank()) {
                return rootKey;
            }
            if (name == null) {
                return null;
            }
            int cut = name.length();
            int dot = name.indexOf('.');
            int bracket = name.indexOf('[');
            if (dot >= 0) cut = Math.min(cut, dot);
            if (bracket >= 0) cut = Math.min(cut, bracket);
            return name.substring(0, cut);
        }

        public String requiredMessage() {
            return (label == null || label.isBlank() ? name : label) + " is required";
        }
    }

    /**
     * Per-category document modals. Any of the three lists may be empty —
     * Contract Manufacturer has no accreditation kind and Consultancy has none of
     * the three.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Accreditations(
            List<Field> accreditation,
            List<Field> certification,
            List<Field> supportDoc) {
    }
}
