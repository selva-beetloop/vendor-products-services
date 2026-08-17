package com.beetloop.vendorproducts.services.dto;

import com.beetloop.vendorproducts.services.domain.ServiceCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Request/response payloads for the vendor Services module. */
public final class ServiceDtos {

    private ServiceDtos() {
    }

    /** POST /services — opens a draft wizard run for a chosen category. */
    @Schema(description = "Creates an empty draft service batch for one of the five categories.")
    public record CreateBatchRequest(
            @NotNull(message = "Category is required")
            @Schema(example = "lab-testing",
                    allowableValues = {"lab-testing", "consultancy", "contract-manufacturer",
                            "agro-processing", "cro"})
            ServiceCategory category) {
    }

    /**
     * PUT /services/{id}/stages/{stageKey} — one wizard step.
     *
     * <p>{@code data} is the batch-level payload for that stage; {@code items}
     * carries per-service payloads for the same stage, which is how Configure
     * Services persists several services at once.
     */
    @Schema(description = "Saves one wizard stage. Batch-level data and/or per-service data.")
    public record StageSaveRequest(
            Map<String, Object> data,
            List<ItemStagePayload> items,
            @Schema(description = "Skip required-field checks so partial progress survives navigation.",
                    defaultValue = "false")
            Boolean draft) {

        public Map<String, Object> dataOrEmpty() {
            return data == null ? new LinkedHashMap<>() : data;
        }

        public List<ItemStagePayload> itemsOrEmpty() {
            return items == null ? new ArrayList<>() : items;
        }

        public boolean isDraft() {
            return Boolean.TRUE.equals(draft);
        }
    }

    /** One service's payload for a stage. */
    @Schema(description = "Per-service payload within a stage save.")
    public record ItemStagePayload(
            @Schema(description = "Existing service id; omit to create a new item in the batch.")
            String id,
            String sourceServiceId,
            Boolean custom,
            String name,
            String sku,
            String categoryLabel,
            String serviceType,
            String deliveryMode,
            String turnaround,
            String region,
            String thumbEmoji,
            String configurationStatus,
            Map<String, Object> data) {

        public Map<String, Object> dataOrEmpty() {
            return data == null ? new LinkedHashMap<>() : data;
        }
    }

    /** POST /services/{id}/save — whole-batch transactional save. */
    @Schema(description = "Whole-batch save. Stages omitted keep whatever step saves stored.")
    public record OverallSaveRequest(
            @Schema(description = "stageKey → batch-level payload")
            Map<String, Map<String, Object>> stages,
            @Schema(description = "Full replacement list of services in this batch")
            List<ServiceItemRequest> items,
            Boolean draft,
            @Schema(description = "Submit for QC in the same call once validation passes.")
            Boolean submitForQc) {

        public Map<String, Map<String, Object>> stagesOrEmpty() {
            return stages == null ? new LinkedHashMap<>() : stages;
        }

        public boolean isDraft() {
            return Boolean.TRUE.equals(draft);
        }

        public boolean isSubmitForQc() {
            return Boolean.TRUE.equals(submitForQc);
        }
    }

    /** A complete service inside an overall save. */
    public record ServiceItemRequest(
            String id,
            String sourceServiceId,
            Boolean custom,
            String name,
            String sku,
            String categoryLabel,
            String serviceType,
            String deliveryMode,
            String turnaround,
            String region,
            String thumbEmoji,
            String configurationStatus,
            @Schema(description = "stageKey → this service's payload for that stage")
            Map<String, Object> stagePayloads,
            List<DocumentRequest> documents) {

        public Map<String, Object> stagePayloadsOrEmpty() {
            return stagePayloads == null ? new LinkedHashMap<>() : stagePayloads;
        }

        public List<DocumentRequest> documentsOrEmpty() {
            return documents == null ? new ArrayList<>() : documents;
        }
    }

    /** Add Accreditation / Add Certification / Add Support Doc. */
    @Schema(description = "A service document. `kind` selects which modal's field set applies.")
    public record DocumentRequest(
            String id,
            @NotBlank(message = "Document kind is required")
            @Schema(example = "CERTIFICATION",
                    allowableValues = {"ACCREDITATION", "CERTIFICATION", "SUPPORT_DOC"})
            String kind,
            String name,
            String issuingBody,
            String referenceNumber,
            String validFrom,
            String validTo,
            String status,
            String fileName,
            String fileId,
            String fileUrl,
            @Schema(description = "Remaining kind- and category-specific fields.")
            Map<String, Object> data) {

        public Map<String, Object> dataOrEmpty() {
            return data == null ? new LinkedHashMap<>() : data;
        }
    }

    /** PUT /services/{id}/qc-decision. */
    public record QcDecisionRequest(
            @NotBlank(message = "Decision is required")
            @Schema(example = "APPROVE", allowableValues = {"APPROVE", "REJECT", "QUERY", "PUBLISH"})
            String decision,
            String reviewer,
            String remarks) {
    }

    // ---------------------------------------------------------------- responses

    @Schema(description = "A wizard run with all its services.")
    public record BatchResponse(
            String id,
            String category,
            String categoryLabel,
            String groupId,
            String status,
            String statusKind,
            String statusLabel,
            int stageCount,
            List<String> stageKeys,
            Map<String, Object> stagePayloads,
            List<ServiceResponse> items,
            QcSection qc,
            Instant createdAt,
            Instant updatedAt) {

        public record QcSection(String reviewer, String remarks, Instant submittedAt, Instant reviewedAt) {
        }
    }

    @Schema(description = "One service with its per-stage payloads and documents.")
    public record ServiceResponse(
            String id,
            int position,
            String sourceServiceId,
            boolean custom,
            String name,
            String sku,
            String categoryLabel,
            String serviceType,
            String deliveryMode,
            String turnaround,
            String region,
            String thumbEmoji,
            String configurationStatus,
            int rfqs,
            Map<String, Object> stagePayloads,
            List<DocumentResponse> documents,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record DocumentResponse(
            String id,
            /** The client's own id for this document, echoed back so a reload can re-link it. */
            String externalRef,
            int position,
            String kind,
            String name,
            String issuingBody,
            String referenceNumber,
            String validFrom,
            String validTo,
            String status,
            String fileName,
            String fileId,
            String fileUrl,
            Map<String, Object> data) {
    }

    /**
     * One row of GET /services. Field names match the frontend's CatalogService
     * interface so the services table can render without a mapping layer.
     */
    @Schema(description = "Services listing row (mirrors the frontend CatalogService).")
    public record ServiceSummaryResponse(
            String id,
            String batchId,
            String sourceServiceId,
            String name,
            String sku,
            String category,
            String serviceType,
            String deliveryMode,
            String turnaround,
            String region,
            @Schema(description = "draft | qc-pending | query | published")
            String status,
            String statusLabel,
            @Schema(description = "Raw state machine value, e.g. SUBMITTED_FOR_QC")
            String statusCode,
            int rfqs,
            @Schema(description = "eye | edit")
            String actionIcon,
            String thumbEmoji,
            String configurationStatus,
            String categoryId,
            String groupId,
            int documentCount,
            Instant createdAt,
            Instant updatedAt) {
    }
}
