package com.beetloop.vendorproducts.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code POST /products/{id}/save} — the "overall save" mode. Sends every
 * completed section in one transactional call; anything omitted keeps whatever
 * the step-based saves already stored.
 */
@Schema(description = "Whole-product save. Sections omitted are left untouched.")
public record OverallSaveRequest(

        @Schema(description = "Step 1 — product / machine identity.")
        IdentityStepRequest productIdentity,

        @Schema(description = "Step 2 — your role & supply information.")
        RoleStepRequest yourRole,

        @Schema(description = "Step 3 — replaces the full variant list when present.")
        List<VariantRequest> variants,

        @Schema(description = "Save as draft (skip required-field checks) instead of a complete save.",
                defaultValue = "false")
        Boolean draft,

        @Schema(description = "Submit for QC in the same call once validation passes.",
                defaultValue = "false")
        Boolean submitForQc) {

    public List<VariantRequest> variantsOrNull() {
        return variants;
    }

    public List<VariantRequest> variantsOrEmpty() {
        return variants == null ? new ArrayList<>() : variants;
    }

    public boolean isDraft() {
        return Boolean.TRUE.equals(draft);
    }

    public boolean isSubmitForQc() {
        return Boolean.TRUE.equals(submitForQc);
    }
}
