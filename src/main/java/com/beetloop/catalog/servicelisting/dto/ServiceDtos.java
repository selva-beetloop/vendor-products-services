package com.beetloop.catalog.servicelisting.dto;

import com.beetloop.catalog.servicelisting.model.ServiceCategoryCode;
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

public final class ServiceDtos {

    private ServiceDtos() {
    }

    public record CreateServiceRequest(@NotNull ServiceCategoryCode categoryCode, EntryPath entryPath) {
    }

    public record ServiceResponse(
            String serviceListingId,
            String code,
            ServiceCategoryCode categoryCode,
            String categoryLabel,
            String categoryId,
            EntryPath entryPath,
            int templateVersion,
            String currentStep,
            List<String> completedSteps,
            Map<String, String> stepLabels,
            List<String> sectionKeys,
            Map<String, Object> data,
            List<ConfigurationResponse> configurations,
            Map<String, Object> derived,
            QcStatus qcStatus,
            Lifecycle lifecycle,
            long version,
            String etag,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record StepReadResponse(String serviceListingId, String stepKey, int templateVersion,
                                   boolean complete, Map<String, Object> data) {
    }

    public record StepSaveRequest(Map<String, Object> data, String currentStep) {
    }

    public record StepSaveResponse(
            String serviceListingId,
            String stepKey,
            long version,
            String etag,
            String currentStep,
            List<String> completedSteps,
            QcStatus qcStatus,
            Instant savedAt,
            Map<String, Object> data,
            String banner,
            List<RejectedField> rejectedFields,
            List<Warning> warnings) {
    }

    public record ValidateStepResponse(String stepKey, boolean valid, boolean complete,
                                       String bannerMessage, List<FieldError> fieldErrors,
                                       List<Warning> warnings) {
    }

    // ------------------------------------------------------------------ overall save

    public record SaveAllRequest(
            String serviceListingId,
            @NotNull ServiceCategoryCode categoryCode,
            String currentStep,
            Map<String, Map<String, Object>> steps,
            List<ConfigurationPayload> configurations) {
    }

    public record ConfigurationPayload(String configurationId, String selectionId,
                                       Map<String, Object> sections) {
    }

    public record SaveAllResponse(
            String serviceListingId,
            String code,
            long version,
            String etag,
            QcStatus qcStatus,
            Lifecycle lifecycle,
            Instant savedAt,
            List<String> savedSteps,
            List<String> clearedSteps,
            List<SavedChild> savedConfigurations,
            List<SavedChild> createdConfigurations,
            List<String> deletedConfigurations,
            List<String> completedSteps,
            Map<String, Object> derived,
            List<RejectedField> rejectedFields,
            List<Warning> warnings) {
    }

    public record SavedChild(String configurationId, boolean created, Integer completionPercent,
                             String configurationStatus) {
    }

    // ------------------------------------------------------------------ configurations

    public record AddConfigurationRequest(String masterServiceId, Boolean requestNew, String name,
                                          String serviceType) {
    }

    public record ConfigurationResponse(
            String configurationId,
            String selectionId,
            String masterServiceId,
            String requestCode,
            String name,
            String serviceType,
            String source,
            List<String> sectionKeys,
            Map<String, Object> sections,
            String configurationStatus,
            Instant configuredAt,
            int completionPercent) {
    }

    /** The Selected Services (N) table on outer step 2. */
    public record ConfigurationRow(
            String configurationId,
            String selectionId,
            int index,
            String name,
            String serviceType,
            String source,
            String sourceBadge,
            List<String> keyParametersAnalytes,
            String standardTat,
            String configurationStatus,
            Instant configuredAt,
            String statusLine,
            int completionPercent,
            String actionUrl) {
    }

    public record SectionSaveRequest(Map<String, Object> data) {
    }

    public record SectionSaveResponse(
            String configurationId,
            String sectionKey,
            long version,
            int completionPercent,
            String configurationStatus,
            Map<String, Object> data,
            Map<String, Object> dependentSchema,
            List<RejectedField> rejectedFields,
            List<Warning> warnings) {
    }

    public record ConfigurationWholeSaveRequest(Map<String, Object> sections) {
    }

    public record ConfigurationWholeSaveResponse(
            String configurationId,
            long version,
            int completionPercent,
            String configurationStatus,
            List<String> savedSections,
            List<RejectedField> rejectedFields,
            List<Warning> warnings) {
    }

    // ------------------------------------------------------------------ review & submit

    public record ReviewResponse(
            String serviceListingId,
            String code,
            String categoryLabel,
            boolean readyToSubmit,
            int completedStepCount,
            int totalStepCount,
            String progressLabel,
            List<StepPanel> steps,
            List<FieldError> blockingErrors,
            List<Warning> warnings) {
    }

    public record StepPanel(String stepKey, String label, List<String> badges, boolean complete,
                            int errorCount, List<SummaryItem> summary, List<SummaryItem> children,
                            List<FieldError> errors, String editUrl) {
    }

    public record SummaryItem(String label, String value) {
    }

    public record SubmitRequest(Boolean acknowledged, String notes) {
    }

    public record SubmitResponse(String serviceListingId, String code, QcStatus qcStatus,
                                 Lifecycle lifecycle, String qcReviewId, int revision,
                                 Instant submittedAt, long submissionVersion, long version) {
    }

    public record WithdrawRequest(String reason) {
    }

    public record WithdrawResponse(String serviceListingId, QcStatus qcStatus, Lifecycle lifecycle,
                                   long version, Instant withdrawnAt) {
    }
}
