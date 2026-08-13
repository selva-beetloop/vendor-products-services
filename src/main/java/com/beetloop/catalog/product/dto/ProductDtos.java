package com.beetloop.catalog.product.dto;

import com.beetloop.catalog.product.model.ProductCategoryCode;
import com.beetloop.catalog.shared.error.FieldError;
import com.beetloop.catalog.shared.error.RejectedField;
import com.beetloop.catalog.shared.error.Warning;
import com.beetloop.catalog.shared.model.EntryPath;
import com.beetloop.catalog.shared.model.Lifecycle;
import com.beetloop.catalog.shared.model.QcStatus;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Request and response bodies for the product endpoints. */
public final class ProductDtos {

    private ProductDtos() {
    }

    // ------------------------------------------------------------------ lifecycle

    public record CreateProductRequest(
            @NotNull ProductCategoryCode categoryCode,
            @NotNull EntryPath entryPath,
            String masterProductId) {
    }

    public record ProductResponse(
            String productId,
            String code,
            ProductCategoryCode categoryCode,
            String categoryId,
            EntryPath entryPath,
            String masterProductId,
            String requestCode,
            int templateVersion,
            String currentStep,
            List<String> completedSteps,
            Map<String, Object> data,
            List<VariantResponse> variants,
            Map<String, Object> derived,
            QcStatus qcStatus,
            Lifecycle lifecycle,
            long version,
            String etag,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record StepReadResponse(
            String productId,
            String stepKey,
            int templateVersion,
            boolean complete,
            Map<String, Object> data) {
    }

    // ------------------------------------------------------------------ step-wise save

    public record StepSaveRequest(
            Map<String, Object> data,
            /** Where the wizard is navigating next. Optional. */
            String currentStep) {
    }

    public record StepSaveResponse(
            String productId,
            String stepKey,
            long version,
            String etag,
            String currentStep,
            List<String> completedSteps,
            QcStatus qcStatus,
            Instant savedAt,
            Map<String, Object> data,
            List<RejectedField> rejectedFields,
            List<Warning> warnings) {
    }

    public record ValidateStepResponse(
            String stepKey,
            boolean valid,
            boolean complete,
            String bannerMessage,
            List<FieldError> fieldErrors,
            List<Warning> warnings) {
    }

    // ------------------------------------------------------------------ overall save

    /**
     * FULL REPLACE. Every step key present is written; every step key ABSENT is cleared. Every
     * variant whose id is absent from `variants` is deleted. Still a draft write: required fields
     * are not enforced and qcStatus is never touched. There is deliberately no `submit` flag.
     */
    public record SaveAllRequest(
            String productId,
            @NotNull ProductCategoryCode categoryCode,
            EntryPath entryPath,
            String masterProductId,
            String currentStep,
            Map<String, Map<String, Object>> steps,
            List<VariantPayload> variants) {
    }

    public record VariantPayload(String variantId, Map<String, Object> sections) {
    }

    public record SaveAllResponse(
            String productId,
            String code,
            long version,
            String etag,
            QcStatus qcStatus,
            Lifecycle lifecycle,
            Instant savedAt,
            List<String> savedSteps,
            List<String> clearedSteps,
            List<SavedChild> savedVariants,
            List<SavedChild> createdVariants,
            List<String> deletedVariants,
            List<String> completedSteps,
            Map<String, Object> derived,
            List<RejectedField> rejectedFields,
            List<Warning> warnings) {
    }

    public record SavedChild(String variantId, boolean created, Integer completionPercent) {
    }

    // ------------------------------------------------------------------ variants

    public record CreateVariantRequest(Map<String, Object> sections) {
    }

    public record VariantResponse(
            String variantId,
            Map<String, Object> sections,
            String status,
            int completionPercent,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record VariantSectionSaveRequest(Map<String, Object> data) {
    }

    public record VariantSectionSaveResponse(
            String variantId,
            String sectionKey,
            long version,
            int completionPercent,
            String status,
            Map<String, Object> data,
            List<RejectedField> rejectedFields,
            List<Warning> warnings) {
    }

    public record VariantWholeSaveRequest(Map<String, Object> sections) {
    }

    public record VariantWholeSaveResponse(
            String variantId,
            long version,
            int completionPercent,
            String status,
            List<String> savedSections,
            List<RejectedField> rejectedFields,
            List<Warning> warnings) {
    }

    public record VariantListMeta(
            Map<String, Object> counters,
            Map<String, Object> groupings,
            List<Map<String, String>> gridColumns,
            List<Map<String, Object>> groups) {
    }

    public record BulkActionRequest(@NotNull String action, List<String> variantIds) {
    }

    public record BulkActionResponse(
            String action,
            int requested,
            int succeeded,
            int failed,
            List<BulkResult> results) {
    }

    public record BulkResult(String id, String status, String code, String message) {
    }

    // ------------------------------------------------------------------ review & submit

    public record ReviewResponse(
            String productId,
            String code,
            String categoryLabel,
            boolean readyToSubmit,
            int completedStepCount,
            int totalStepCount,
            Map<String, Object> headerCard,
            List<StepPanel> steps,
            List<FieldError> blockingErrors,
            List<Warning> warnings,
            String note) {
    }

    public record StepPanel(
            String stepKey,
            String label,
            List<String> badges,
            boolean complete,
            int errorCount,
            List<SummaryItem> summary,
            List<FieldError> errors,
            String editUrl) {
    }

    public record SummaryItem(String label, String value) {
    }

    public record SubmitRequest(Boolean acknowledged, String notes) {
    }

    public record SubmitResponse(
            String productId,
            String code,
            QcStatus qcStatus,
            Lifecycle lifecycle,
            String qcReviewId,
            int revision,
            Instant submittedAt,
            long submissionVersion,
            long version) {
    }

    public record WithdrawRequest(String reason) {
    }

    public record WithdrawResponse(
            String productId,
            QcStatus qcStatus,
            Lifecycle lifecycle,
            long version,
            Instant withdrawnAt) {
    }
}
