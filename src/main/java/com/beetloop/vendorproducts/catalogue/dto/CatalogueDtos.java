package com.beetloop.vendorproducts.catalogue.dto;

import com.beetloop.vendorproducts.catalogue.domain.CatalogueKind;
import com.beetloop.vendorproducts.catalogue.domain.CatalogueStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class CatalogueDtos {

    private CatalogueDtos() {
    }

    public record ScientificMasterResponse(
            UUID id,
            String code,
            CatalogueKind kind,
            String category,
            String name,
            String casNumber,
            String formula,
            CatalogueStatus status,
            Map<String, Object> payload,
            String qcReviewer,
            String qcRemarks,
            Instant reviewedAt,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record CommercialMasterResponse(
            UUID id,
            String code,
            UUID scientificMasterId,
            String scientificMasterCode,
            CatalogueKind kind,
            String category,
            String name,
            String assay,
            String grade,
            String form,
            String origin,
            String colour,
            String source,
            CatalogueStatus status,
            Map<String, Object> baseline,
            String parentCode,
            String qcReviewer,
            String qcRemarks,
            Instant reviewedAt,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record CreateScientificRequest(
            CatalogueKind kind,
            String category,
            String name,
            String casNumber,
            String formula,
            Map<String, Object> payload) {
    }

    public record CreateCommercialRequest(
            UUID scientificMasterId,
            String scientificMasterCode,
            CatalogueKind kind,
            String category,
            String name,
            String assay,
            String grade,
            String form,
            String origin,
            String colour,
            String source,
            Map<String, Object> baseline) {
    }

    public record BranchRequest(
            String assay,
            String grade,
            String form,
            String origin,
            String colour,
            String source,
            String name) {
    }

    public record IntelQcDecisionRequest(
            @Schema(allowableValues = {"APPROVE", "REJECT", "QUERY", "PUBLISH"})
            String decision,
            String reviewer,
            String remarks,
            String kind,
            UUID id) {
    }

    public record IntelQcRow(
            UUID id,
            String code,
            String kind,
            String layer,
            String name,
            String category,
            CatalogueStatus status,
            Instant updatedAt) {
    }
}
