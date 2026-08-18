package com.beetloop.vendorproducts.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * One row of {@code GET /products}. Field names deliberately match the frontend's
 * {@code CatalogProduct} interface so the catalog table can render a row without
 * a mapping layer.
 */
@Schema(description = "Catalog listing row (mirrors the frontend CatalogProduct).")
public record ProductSummaryResponse(

        String id,
        String name,
        String sku,
        boolean hasTypeBadge,
        boolean verified,
        @Schema(description = "Human label — Material / Finished Good / Packaging Material / …")
        String type,
        String groupId,
        String category,
        List<String> functionalRole,
        List<String> applications,
        String originFlag,
        String originCountry,
        List<String> documents,
        int documentsExtra,
        @Schema(description = "available | on-request")
        String sample,
        String inventoryQty,
        String inventoryBatches,
        @Schema(description = "draft | submitted | qc-pending | query | rejected | approved | published")
        String status,
        String statusLabel,
        @Schema(description = "Raw state machine value, e.g. SUBMITTED_FOR_QC")
        String statusCode,
        @Schema(description = "eye | edit")
        String actionIcon,
        String thumbEmoji,
        String thumbImage,
        int variantCount,
        Instant createdAt,
        Instant updatedAt,
        String vendorId,
        String qcRemarks) {
}
