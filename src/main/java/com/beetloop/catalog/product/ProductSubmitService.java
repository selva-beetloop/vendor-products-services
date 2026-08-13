package com.beetloop.catalog.product;

import com.beetloop.catalog.audit.AuditEvent;
import com.beetloop.catalog.audit.AuditService;
import com.beetloop.catalog.product.dto.ProductDtos;
import com.beetloop.catalog.product.model.ProductListing;
import com.beetloop.catalog.product.model.ProductStepKey;
import com.beetloop.catalog.qc.QcReview;
import com.beetloop.catalog.qc.QcReviewService;
import com.beetloop.catalog.shared.error.ApiException;
import com.beetloop.catalog.shared.error.ErrorCode;
import com.beetloop.catalog.shared.error.FieldError;
import com.beetloop.catalog.shared.error.ValidationException;
import com.beetloop.catalog.shared.model.Lifecycle;
import com.beetloop.catalog.shared.model.QcStatus;
import com.beetloop.catalog.shared.tenant.TenantContext;
import com.beetloop.catalog.template.TemplateService;
import com.beetloop.catalog.template.model.FormTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The only gate.
 *
 * The Review page renders "Complete" for a wizard with every field empty, and "Submit for QC" on
 * Lab Testing produced no network call at all. So this re-validates every step, every
 * category-specific required field, every conditional rule, every document and every variant from
 * scratch, and returns all errors at once - the client has no error state of its own to fall back on.
 */
@Service
public class ProductSubmitService {

    private final ProductListingRepository repository;
    private final ProductGuard guard;
    private final TemplateService templates;
    private final ProductReviewService reviewService;
    private final ProductRecalculator recalculator;
    private final QcReviewService qcReviewService;
    private final AuditService audit;

    public ProductSubmitService(ProductListingRepository repository, ProductGuard guard,
                                TemplateService templates, ProductReviewService reviewService,
                                ProductRecalculator recalculator, QcReviewService qcReviewService,
                                AuditService audit) {
        this.repository = repository;
        this.guard = guard;
        this.templates = templates;
        this.reviewService = reviewService;
        this.recalculator = recalculator;
        this.qcReviewService = qcReviewService;
        this.audit = audit;
    }

    @Transactional
    public ProductDtos.SubmitResponse submit(String productId, ProductDtos.SubmitRequest request) {
        ProductListing listing = guard.load(productId);

        if (listing.getQcStatus() == QcStatus.PENDING_REVIEW
                || listing.getQcStatus() == QcStatus.IN_REVIEW
                || listing.getQcStatus() == QcStatus.APPROVED) {
            QcReview open = qcReviewService.queue(listing.getQcStatus(), null, null,
                            org.springframework.data.domain.PageRequest.of(0, 1))
                    .stream().findFirst().orElse(null);
            throw new ApiException(ErrorCode.ALREADY_SUBMITTED,
                    "This listing is already %s.".formatted(listing.getQcStatus()))
                    .with("qcStatus", listing.getQcStatus())
                    .with("qcReviewId", open == null ? null : open.getId());
        }

        FormTemplate template = templates.forListing(listing.getCategoryCode().name(),
                listing.getTemplateVersion());
        // Re-derive before validating, so no stale derived value can pass a check it should fail.
        recalculator.recompute(listing, template);

        List<FieldError> errors = new ArrayList<>();
        for (String stepKey : ProductStepKey.COMPLETABLE) {
            template.step(stepKey).ifPresent(step ->
                    errors.addAll(reviewService.errorsFor(listing, template, step, stepKey)));
        }
        if (!Boolean.TRUE.equals(request.acknowledged())) {
            errors.add(FieldError.of(ProductStepKey.REVIEW, "acknowledged", "Acknowledgement",
                    "FIELD_REQUIRED", "Confirm the pre-submission checklist before submitting."));
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors, List.of());
        }

        Instant now = Instant.now();
        listing.setQcStatus(QcStatus.PENDING_REVIEW);
        listing.setLifecycle(Lifecycle.SUBMITTED);
        listing.setSubmittedAt(now);
        listing.setSubmittedBy(TenantContext.userId());
        listing.setRevision(listing.getRevision() + 1);
        ProductListing saved = repository.save(listing);

        QcReview review = qcReviewService.openReview("PRODUCT_LISTING", saved.getId(), saved.getCode(),
                saved.getVendorId(), saved.getCategoryCode().name(),
                String.valueOf(saved.getSearch().get("name")), saved.versionOrZero());

        audit.record(AuditEvent.PRODUCT_SUBMITTED, "PRODUCT_LISTING", saved.getId(),
                Map.of("qcReviewId", review.getId(), "revision", review.getRevision(),
                        "notes", request.notes() == null ? "" : request.notes()));

        return new ProductDtos.SubmitResponse(saved.getId(), saved.getCode(), saved.getQcStatus(),
                saved.getLifecycle(), review.getId(), review.getRevision(), now,
                review.getSubmissionVersion(), saved.versionOrZero());
    }

    @Transactional
    public ProductDtos.WithdrawResponse withdraw(String productId, ProductDtos.WithdrawRequest request) {
        ProductListing listing = guard.load(productId);
        if (listing.getQcStatus() != QcStatus.PENDING_REVIEW) {
            throw new ApiException(ErrorCode.NOT_SUBMITTED,
                    "Only a listing awaiting review can be withdrawn. This one is %s."
                            .formatted(listing.getQcStatus()));
        }
        listing.setQcStatus(null);
        listing.setLifecycle(Lifecycle.DRAFT);
        listing.setSubmittedAt(null);
        ProductListing saved = repository.save(listing);

        qcReviewService.recordWithdrawal("PRODUCT_LISTING", saved.getId(), request.reason());
        audit.record(AuditEvent.PRODUCT_WITHDRAWN, "PRODUCT_LISTING", saved.getId(),
                Map.of("reason", request.reason() == null ? "" : request.reason()));

        return new ProductDtos.WithdrawResponse(saved.getId(), saved.getQcStatus(),
                saved.getLifecycle(), saved.versionOrZero(), Instant.now());
    }
}
