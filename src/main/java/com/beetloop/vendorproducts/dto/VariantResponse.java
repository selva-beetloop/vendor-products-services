package com.beetloop.vendorproducts.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * A saved variant, echoed back in exactly the same nesting the wizard sends, so
 * the frontend can rehydrate a variant without reshaping anything.
 */
@Schema(description = "A saved variant with all five sub-steps.")
public record VariantResponse(

        String id,
        int position,
        VariantRequest.VariantDetails variantDetails,
        VariantRequest.TechnicalSpecifications technicalSpecifications,
        VariantRequest.CommercialPricingDto commercialPricing,
        VariantRequest.ComplianceCertifications complianceCertifications,
        Map<String, Object> searchMarketplace,

        @Schema(description = "Flat summary used by the Variants & Pack Sizes listing table.")
        Summary summary,

        Instant createdAt,
        Instant updatedAt) {

    @Schema(description = "Denormalised row for the variants listing.")
    public record Summary(
            String name,
            String variantType,
            String grade,
            String assayPurity,
            String packSize,
            String packagingType,
            String particleSize,
            String skuCode,
            String batchPrefix,
            String status,
            List<String> images,
            int specificationCount,
            int parameterCount,
            int documentCount) {
    }
}
