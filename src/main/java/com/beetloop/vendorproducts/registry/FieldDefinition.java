package com.beetloop.vendorproducts.registry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * One control on a wizard screen, as observed in the frontend.
 *
 * @param name         the key used in the JSON payload (matches the frontend form-state key)
 * @param label        the visible label, used to build validation messages
 * @param type         text | textarea | select | multiselect | number | date | checkbox |
 *                     toggle | tags | image | file | repeatable | object
 * @param required     whether the frontend blocks Save &amp; Continue when empty
 * @param options      dropdown/checkbox options where the UI has a fixed list
 * @param multiple     for image/file controls that accept more than one file
 * @param dependsOn    name of the field this one is driven by (e.g. sector depends on industry)
 * @param errorMessage overrides the default "{label} is required" message
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FieldDefinition(
        String name,
        String label,
        String type,
        boolean required,
        List<String> options,
        boolean multiple,
        String dependsOn,
        String errorMessage) {

    public String requiredMessage() {
        if (errorMessage != null && !errorMessage.isBlank()) {
            return errorMessage;
        }
        return (label == null || label.isBlank() ? name : label) + " is required";
    }
}
