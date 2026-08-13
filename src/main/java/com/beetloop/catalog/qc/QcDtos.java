package com.beetloop.catalog.qc;

import com.beetloop.catalog.shared.model.Lifecycle;
import com.beetloop.catalog.shared.model.QcStatus;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class QcDtos {

    private QcDtos() {
    }

    public record QueueItem(
            String reviewId,
            String entityType,
            String entityId,
            String entityCode,
            String listingName,
            String categoryCode,
            String vendorId,
            QcStatus status,
            String priority,
            int revision,
            Instant submittedAt,
            Double waitingHours,
            String claimedBy) {
    }

    public record ReviewDetail(
            String reviewId,
            QcStatus status,
            int revision,
            Instant submittedAt,
            long submissionVersion,
            Object listing,
            Map<String, Object> validationReport,
            String claimedBy,
            Instant claimExpiresAt) {
    }

    public record ClaimResponse(
            String reviewId,
            QcStatus status,
            String claimedBy,
            Instant claimedAt,
            Instant claimExpiresAt) {
    }

    public record ApproveRequest(String notes, Boolean approveMasterRequest) {
    }

    /** A rejection MUST name at least one field, otherwise the vendor has nothing to act on. */
    public record RejectRequest(@NotBlank String reason, List<QcReview.FieldFeedback> fieldFeedback) {
    }

    public record DecisionResponse(
            String reviewId,
            QcStatus status,
            String entityId,
            QcStatus listingQcStatus,
            Lifecycle listingLifecycle,
            String decidedBy,
            Instant decidedAt,
            Instant publishedAt,
            boolean vendorNotified) {
    }

    public record HistoryRow(
            QcStatus fromStatus,
            QcStatus toStatus,
            String actorId,
            String actorRole,
            String reason,
            String requestId,
            Instant at) {
    }
}
