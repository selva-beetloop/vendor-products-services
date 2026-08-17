package com.beetloop.vendorproducts.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Full nested product document returned by every write endpoint and by {@code GET /products/{id}}. */
@Schema(description = "Complete product with all wizard sections.")
public record ProductResponse(

        String id,
        @Schema(description = "T3 listing code VCG-…-V### when a T2 is attached.")
        String code,
        String listingCode,
        String category,
        @Schema(description = "Catalog chip id — materials | finished | packaging-materials | "
                + "packaging-machinery | processing-machinery")
        String groupId,
        String status,
        @Schema(description = "Frontend StatusKind — draft | qc-pending | query | published")
        String statusKind,
        String statusLabel,

        String name,
        String sku,
        String listingCategory,
        String originCountry,
        String thumbEmoji,
        String thumbImage,
        boolean verified,

        String sourceMasterId,
        String commercialMasterId,
        String commercialMasterCode,
        String scientificMasterId,
        String scientificMasterCode,
        boolean holdPublish,

        @Schema(description = "Step 1 as saved.")
        IdentitySection productIdentity,

        @Schema(description = "Step 2 as saved.")
        RoleSection yourRole,

        @Schema(description = "Step 3 as saved.")
        List<VariantResponse> variants,

        QcSection qc,

        @Schema(description = "Which wizard steps are complete — drives the stepper ticks.")
        Completion completion,

        Instant createdAt,
        Instant updatedAt) {

    public record IdentitySection(String identityType, Map<String, Object> data) {
    }

    public record RoleSection(String roleId, Map<String, Object> data) {
    }

    public record QcSection(String reviewer, String remarks, Instant submittedAt, Instant reviewedAt) {
    }

    public record Completion(boolean identity, boolean role, boolean variants, boolean submitted) {
    }
}
