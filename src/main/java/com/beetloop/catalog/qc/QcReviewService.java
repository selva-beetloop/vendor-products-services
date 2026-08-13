package com.beetloop.catalog.qc;

import com.beetloop.catalog.audit.AuditEvent;
import com.beetloop.catalog.audit.AuditService;
import com.beetloop.catalog.config.CatalogProperties;
import com.beetloop.catalog.shared.error.ApiException;
import com.beetloop.catalog.shared.error.ErrorCode;
import com.beetloop.catalog.shared.model.Lifecycle;
import com.beetloop.catalog.shared.model.QcStatus;
import com.beetloop.catalog.shared.tenant.TenantContext;
import com.beetloop.catalog.shared.util.Ids;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 *   qcStatus: null --submit-qc--> PENDING_REVIEW --claim--> IN_REVIEW --approve--> APPROVED
 *        ^                              |                        |                    |
 *        |                              | withdraw               | reject             | publish
 *        +------------------------------+                        v                    v
 *                                                            REJECTED            PUBLISHED
 *
 * Only PENDING_REVIEW can be claimed; approve and reject require the claim. Claiming is what stops
 * two reviewers duplicating work.
 */
@Service
public class QcReviewService {

    private final QcReviewRepository reviews;
    private final StatusHistoryRepository history;
    private final List<ReviewableListingPort> ports;
    private final CatalogProperties properties;
    private final AuditService audit;

    public QcReviewService(QcReviewRepository reviews, StatusHistoryRepository history,
                           List<ReviewableListingPort> ports, CatalogProperties properties,
                           AuditService audit) {
        this.reviews = reviews;
        this.history = history;
        this.ports = ports;
        this.properties = properties;
        this.audit = audit;
    }

    // ------------------------------------------------------------------ submission

    /** Called inside the submit transaction. A listing marked PENDING_REVIEW with no review item is invisible to QC. */
    public QcReview openReview(String entityType, String entityId, String entityCode, String vendorId,
                               String categoryCode, String listingName, long submissionVersion) {
        int revision = reviews.findFirstByEntityIdOrderByRevisionDesc(entityId)
                .map(r -> r.getRevision() + 1)
                .orElse(1);
        Instant now = Instant.now();
        QcReview review = QcReview.builder()
                .id(Ids.newId("qcr"))
                .entityType(entityType)
                .entityId(entityId)
                .entityCode(entityCode)
                .vendorId(vendorId)
                .categoryCode(categoryCode)
                .listingName(listingName)
                .status(QcStatus.PENDING_REVIEW)
                .priority("NORMAL")
                .submittedAt(now)
                .submittedBy(TenantContext.userId())
                .submissionVersion(submissionVersion)
                .revision(revision)
                .createdAt(now)
                .updatedAt(now)
                .build();
        reviews.save(review);
        recordHistory(entityType, entityId, null, QcStatus.PENDING_REVIEW, "VENDOR", null);
        return review;
    }

    public void recordWithdrawal(String entityType, String entityId, String reason) {
        recordHistory(entityType, entityId, QcStatus.PENDING_REVIEW, null, "VENDOR", reason);
        reviews.findFirstByEntityIdOrderByRevisionDesc(entityId).ifPresent(reviews::delete);
    }

    // ------------------------------------------------------------------ queue

    public Page<QcReview> queue(QcStatus status, String categoryCode, String entityType, Pageable pageable) {
        QcStatus effective = status == null ? QcStatus.PENDING_REVIEW : status;
        if (categoryCode != null) {
            return reviews.findByStatusAndCategoryCode(effective, categoryCode, pageable);
        }
        if (entityType != null) {
            return reviews.findByStatusAndEntityType(effective, entityType, pageable);
        }
        return reviews.findByStatus(effective, pageable);
    }

    public QcReview require(String reviewId) {
        return reviews.findById(reviewId).orElseThrow(() -> ApiException.notFound("Review " + reviewId));
    }

    public Optional<ReviewableListingPort.Snapshot> snapshot(QcReview review) {
        return port(review.getEntityType()).find(review.getEntityId());
    }

    // ------------------------------------------------------------------ transitions

    @Transactional
    public QcReview claim(String reviewId) {
        QcReview review = require(reviewId);
        releaseIfClaimExpired(review);
        if (review.getStatus() != QcStatus.PENDING_REVIEW) {
            throw new ApiException(ErrorCode.ALREADY_CLAIMED,
                    "Claimed by %s until %s.".formatted(review.getClaimedBy(), review.getClaimExpiresAt()));
        }
        Instant now = Instant.now();
        review.setStatus(QcStatus.IN_REVIEW);
        review.setClaimedBy(TenantContext.userId());
        review.setClaimedAt(now);
        review.setClaimExpiresAt(now.plus(Duration.ofMinutes(properties.getQc().getClaimTimeoutMinutes())));
        review.setUpdatedAt(now);
        reviews.save(review);
        applyToListing(review, QcStatus.IN_REVIEW, null);
        recordHistory(review.getEntityType(), review.getEntityId(), QcStatus.PENDING_REVIEW,
                QcStatus.IN_REVIEW, "QC_REVIEWER", null);
        audit.record(AuditEvent.QC_CLAIMED, review.getEntityType(), review.getEntityId(),
                Map.of("reviewId", review.getId()));
        return review;
    }

    @Transactional
    public QcReview release(String reviewId) {
        QcReview review = require(reviewId);
        review.setStatus(QcStatus.PENDING_REVIEW);
        review.setClaimedBy(null);
        review.setClaimedAt(null);
        review.setClaimExpiresAt(null);
        review.setUpdatedAt(Instant.now());
        reviews.save(review);
        applyToListing(review, QcStatus.PENDING_REVIEW, null);
        recordHistory(review.getEntityType(), review.getEntityId(), QcStatus.IN_REVIEW,
                QcStatus.PENDING_REVIEW, "QC_REVIEWER", null);
        return review;
    }

    @Transactional
    public QcReview approve(String reviewId, String notes) {
        QcReview review = requireClaimed(reviewId);
        Instant now = Instant.now();
        review.setStatus(QcStatus.APPROVED);
        review.setDecidedBy(TenantContext.userId());
        review.setDecidedAt(now);
        review.setDecisionReason(notes);
        review.setUpdatedAt(now);
        reviews.save(review);
        applyToListing(review, QcStatus.APPROVED, Lifecycle.PUBLISHED);
        recordHistory(review.getEntityType(), review.getEntityId(), QcStatus.IN_REVIEW,
                QcStatus.APPROVED, "QC_REVIEWER", notes);
        audit.record(AuditEvent.QC_APPROVED, review.getEntityType(), review.getEntityId(),
                Map.of("reviewId", review.getId(), "notes", notes == null ? "" : notes));
        return review;
    }

    @Transactional
    public QcReview reject(String reviewId, String reason, List<QcReview.FieldFeedback> feedback) {
        if (feedback == null || feedback.isEmpty()) {
            throw new ApiException(ErrorCode.FEEDBACK_REQUIRED,
                    "A rejection must name at least one field so the vendor knows what to fix.");
        }
        QcReview review = requireClaimed(reviewId);
        Instant now = Instant.now();
        review.setStatus(QcStatus.REJECTED);
        review.setDecidedBy(TenantContext.userId());
        review.setDecidedAt(now);
        review.setDecisionReason(reason);
        review.setFieldFeedback(feedback);
        review.setUpdatedAt(now);
        reviews.save(review);
        // A REJECTED listing becomes editable again; the save endpoints unlock.
        applyToListing(review, QcStatus.REJECTED, Lifecycle.DRAFT);
        recordHistory(review.getEntityType(), review.getEntityId(), QcStatus.IN_REVIEW,
                QcStatus.REJECTED, "QC_REVIEWER", reason);
        audit.record(AuditEvent.QC_REJECTED, review.getEntityType(), review.getEntityId(),
                Map.of("reviewId", review.getId(), "reason", reason));
        return review;
    }

    public List<StatusHistory> history(String reviewId) {
        QcReview review = require(reviewId);
        return history.findByEntityIdOrderByAtAsc(review.getEntityId());
    }

    // ------------------------------------------------------------------ internals

    private QcReview requireClaimed(String reviewId) {
        QcReview review = require(reviewId);
        releaseIfClaimExpired(review);
        if (review.getStatus() != QcStatus.IN_REVIEW) {
            throw new ApiException(ErrorCode.NOT_CLAIMED,
                    "Claim the review before approving or rejecting it.");
        }
        if (!TenantContext.userId().equals(review.getClaimedBy())) {
            throw new ApiException(ErrorCode.ALREADY_CLAIMED,
                    "This review is claimed by %s.".formatted(review.getClaimedBy()));
        }
        return review;
    }

    /** An unreleased claim past its timeout returns to the queue, and that return is itself a history row. */
    private void releaseIfClaimExpired(QcReview review) {
        if (review.getStatus() == QcStatus.IN_REVIEW && review.getClaimExpiresAt() != null
                && review.getClaimExpiresAt().isBefore(Instant.now())) {
            review.setStatus(QcStatus.PENDING_REVIEW);
            review.setClaimedBy(null);
            review.setClaimedAt(null);
            review.setClaimExpiresAt(null);
            reviews.save(review);
            recordHistory(review.getEntityType(), review.getEntityId(), QcStatus.IN_REVIEW,
                    QcStatus.PENDING_REVIEW, "SYSTEM", "Claim timed out");
        }
    }

    private void applyToListing(QcReview review, QcStatus status, Lifecycle lifecycle) {
        port(review.getEntityType()).applyDecision(review.getEntityId(), status, lifecycle);
    }

    private ReviewableListingPort port(String entityType) {
        return ports.stream().filter(p -> p.entityType().equals(entityType)).findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL,
                        "No listing port registered for " + entityType));
    }

    private void recordHistory(String entityType, String entityId, QcStatus from, QcStatus to,
                               String actorRole, String reason) {
        history.save(StatusHistory.builder()
                .id(Ids.newId("sth"))
                .entityType(entityType)
                .entityId(entityId)
                .fromStatus(from)
                .toStatus(to)
                .actorId(TenantContext.currentOrNull() == null ? null : TenantContext.current().userId())
                .actorRole(actorRole)
                .reason(reason)
                .requestId(TenantContext.requestId())
                .at(Instant.now())
                .build());
    }
}
