package com.beetloop.vendorproducts.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code PUT /products/{id}/identity} — Step 1 "Save &amp; Continue".
 *
 * <p>{@code data} carries exactly the form-state object the wizard already holds
 * (for Raw Materials that is the base {@code MaterialIdentificationData} merged
 * with the selected type card's own form data). Keys are validated against the
 * registry, so an unknown key is reported rather than silently stored.
 */
@Schema(description = "Step 1 (Product / Machine Identity) payload.")
public record IdentityStepRequest(

        @Schema(description = "Type card chosen in the type selector — required for categories that have one.",
                example = "botanical-extract")
        String identityType,

        @Schema(description = "Flat map of identity field name → value, matching the frontend form state.")
        Map<String, Object> data,

        @Schema(description = "When true, only stores what was sent and skips required-field checks "
                + "so the vendor can leave the wizard and come back.", defaultValue = "false")
        Boolean draft) {

    public Map<String, Object> dataOrEmpty() {
        return data == null ? new LinkedHashMap<>() : data;
    }

    public boolean isDraft() {
        return Boolean.TRUE.equals(draft);
    }
}
