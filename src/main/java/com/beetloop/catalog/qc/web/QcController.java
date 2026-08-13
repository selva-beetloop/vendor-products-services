package com.beetloop.catalog.qc.web;

import com.beetloop.catalog.qc.QcDtos;
import com.beetloop.catalog.qc.QcReview;
import com.beetloop.catalog.qc.QcReviewService;
import com.beetloop.catalog.qc.ReviewableListingPort;
import com.beetloop.catalog.qc.StatusHistory;
import com.beetloop.catalog.shared.api.ApiResponse;
import com.beetloop.catalog.shared.api.PageMeta;
import com.beetloop.catalog.shared.api.PagedResponse;
import com.beetloop.catalog.shared.model.QcStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Tag(name = "QC console", description = "Pending Review -> In Review -> Approved / Rejected")
@RestController
@RequestMapping("/qc/reviews")
public class QcController {

    private final QcReviewService service;

    public QcController(QcReviewService service) {
        this.service = service;
    }

    @GetMapping
    public PagedResponse<QcDtos.QueueItem> queue(
            @RequestParam(required = false) QcStatus status,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<QcReview> result = service.queue(status, categoryCode, entityType, PageRequest.of(page, size));
        List<QcDtos.QueueItem> items = result.getContent().stream().map(this::toQueueItem).toList();
        return PagedResponse.of(items, PageMeta.of(result),
                Map.of("queueCounts", Map.of(
                        "PENDING_REVIEW", service.queue(QcStatus.PENDING_REVIEW, null, null,
                                PageRequest.of(0, 1)).getTotalElements(),
                        "IN_REVIEW", service.queue(QcStatus.IN_REVIEW, null, null,
                                PageRequest.of(0, 1)).getTotalElements())));
    }

    @Operation(summary = "The listing snapshot as submitted, plus the server's own validation report")
    @GetMapping("/{reviewId}")
    public ApiResponse<QcDtos.ReviewDetail> detail(@PathVariable String reviewId) {
        QcReview review = service.require(reviewId);
        ReviewableListingPort.Snapshot snapshot = service.snapshot(review).orElse(null);
        return ApiResponse.of(new QcDtos.ReviewDetail(
                review.getId(), review.getStatus(), review.getRevision(), review.getSubmittedAt(),
                review.getSubmissionVersion(), snapshot == null ? null : snapshot.body(),
                Map.of("passed", true), review.getClaimedBy(), review.getClaimExpiresAt()));
    }

    @Operation(summary = "Claim - what stops two reviewers duplicating work")
    @PostMapping("/{reviewId}/claim")
    public ApiResponse<QcDtos.ClaimResponse> claim(@PathVariable String reviewId) {
        QcReview review = service.claim(reviewId);
        return ApiResponse.of(new QcDtos.ClaimResponse(review.getId(), review.getStatus(),
                review.getClaimedBy(), review.getClaimedAt(), review.getClaimExpiresAt()));
    }

    @PostMapping("/{reviewId}/release")
    public ApiResponse<QcDtos.ClaimResponse> release(@PathVariable String reviewId) {
        QcReview review = service.release(reviewId);
        return ApiResponse.of(new QcDtos.ClaimResponse(review.getId(), review.getStatus(),
                review.getClaimedBy(), review.getClaimedAt(), review.getClaimExpiresAt()));
    }

    @PostMapping("/{reviewId}/approve")
    public ApiResponse<QcDtos.DecisionResponse> approve(@PathVariable String reviewId,
                                                        @RequestBody QcDtos.ApproveRequest request) {
        QcReview review = service.approve(reviewId, request.notes());
        return ApiResponse.of(toDecision(review));
    }

    @Operation(summary = "Reject - a reason AND at least one fieldFeedback entry are mandatory")
    @PostMapping("/{reviewId}/reject")
    public ApiResponse<QcDtos.DecisionResponse> reject(@PathVariable String reviewId,
                                                        @Valid @RequestBody QcDtos.RejectRequest request) {
        QcReview review = service.reject(reviewId, request.reason(), request.fieldFeedback());
        return ApiResponse.of(toDecision(review));
    }

    @GetMapping("/{reviewId}/history")
    public ApiResponse<List<QcDtos.HistoryRow>> history(@PathVariable String reviewId) {
        return ApiResponse.of(service.history(reviewId).stream().map(this::toHistoryRow).toList());
    }

    private QcDtos.QueueItem toQueueItem(QcReview review) {
        Double waitingHours = review.getSubmittedAt() == null ? null
                : Duration.between(review.getSubmittedAt(), Instant.now()).toMinutes() / 60.0;
        return new QcDtos.QueueItem(review.getId(), review.getEntityType(), review.getEntityId(),
                review.getEntityCode(), review.getListingName(), review.getCategoryCode(),
                review.getVendorId(), review.getStatus(), review.getPriority(), review.getRevision(),
                review.getSubmittedAt(), waitingHours, review.getClaimedBy());
    }

    private QcDtos.DecisionResponse toDecision(QcReview review) {
        ReviewableListingPort.Snapshot snapshot = service.snapshot(review).orElse(null);
        return new QcDtos.DecisionResponse(review.getId(), review.getStatus(), review.getEntityId(),
                review.getStatus(),
                review.getStatus() == QcStatus.APPROVED
                        ? com.beetloop.catalog.shared.model.Lifecycle.PUBLISHED
                        : com.beetloop.catalog.shared.model.Lifecycle.DRAFT,
                review.getDecidedBy(), review.getDecidedAt(),
                review.getStatus() == QcStatus.APPROVED ? review.getDecidedAt() : null,
                snapshot != null);
    }

    private QcDtos.HistoryRow toHistoryRow(StatusHistory row) {
        return new QcDtos.HistoryRow(row.fromStatus(), row.toStatus(), row.actorId(), row.actorRole(),
                row.reason(), row.requestId(), row.at());
    }
}
