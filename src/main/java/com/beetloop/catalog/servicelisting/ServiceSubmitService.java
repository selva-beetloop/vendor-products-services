package com.beetloop.catalog.servicelisting;

import com.beetloop.catalog.audit.AuditEvent;
import com.beetloop.catalog.audit.AuditService;
import com.beetloop.catalog.qc.QcReview;
import com.beetloop.catalog.qc.QcReviewService;
import com.beetloop.catalog.servicelisting.dto.ServiceDtos;
import com.beetloop.catalog.servicelisting.model.ServiceListing;
import com.beetloop.catalog.servicelisting.model.ServiceStepKey;
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
 * The UI labels this "Submit for QC" on Lab Testing, Consultancy and CRO, "Submit for review" on
 * Contract Manufacturer, and "Review & Submit" on Agro Cluster. One endpoint, three labels.
 */
@Service
public class ServiceSubmitService {

    private final ServiceListingRepository repository;
    private final ServiceGuard guard;
    private final TemplateService templates;
    private final ServiceReviewService reviewService;
    private final ServiceRecalculator recalculator;
    private final QcReviewService qcReviewService;
    private final AuditService audit;

    public ServiceSubmitService(ServiceListingRepository repository, ServiceGuard guard,
                                TemplateService templates, ServiceReviewService reviewService,
                                ServiceRecalculator recalculator, QcReviewService qcReviewService,
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
    public ServiceDtos.SubmitResponse submit(String serviceListingId, ServiceDtos.SubmitRequest request) {
        ServiceListing listing = guard.load(serviceListingId);
        if (listing.getQcStatus() == QcStatus.PENDING_REVIEW
                || listing.getQcStatus() == QcStatus.IN_REVIEW
                || listing.getQcStatus() == QcStatus.APPROVED) {
            throw new ApiException(ErrorCode.ALREADY_SUBMITTED,
                    "This listing is already %s.".formatted(listing.getQcStatus()))
                    .with("qcStatus", listing.getQcStatus());
        }

        FormTemplate template = templates.forListing(listing.getCategoryCode().name(),
                listing.getTemplateVersion());
        recalculator.recompute(listing, template);

        List<FieldError> errors = new ArrayList<>();
        for (String stepKey : ServiceStepKey.COMPLETABLE) {
            template.step(stepKey).ifPresent(step ->
                    errors.addAll(reviewService.errorsFor(listing, template, step, stepKey)));
        }
        if (!Boolean.TRUE.equals(request.acknowledged())) {
            errors.add(FieldError.of(ServiceStepKey.REVIEW, "acknowledged", "Acknowledgement",
                    "FIELD_REQUIRED", "Confirm the review before submitting."));
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors, reviewService.warnings(listing));
        }

        Instant now = Instant.now();
        listing.setQcStatus(QcStatus.PENDING_REVIEW);
        listing.setLifecycle(Lifecycle.SUBMITTED);
        listing.setSubmittedAt(now);
        listing.setSubmittedBy(TenantContext.userId());
        listing.setRevision(listing.getRevision() + 1);
        ServiceListing saved = repository.save(listing);

        QcReview review = qcReviewService.openReview("SERVICE_LISTING", saved.getId(), saved.getCode(),
                saved.getVendorId(), saved.getCategoryCode().name(),
                String.valueOf(saved.getSearch().get("name")), saved.versionOrZero());

        audit.record(AuditEvent.SERVICE_SUBMITTED, "SERVICE_LISTING", saved.getId(),
                Map.of("qcReviewId", review.getId(), "revision", review.getRevision()));

        return new ServiceDtos.SubmitResponse(saved.getId(), saved.getCode(), saved.getQcStatus(),
                saved.getLifecycle(), review.getId(), review.getRevision(), now,
                review.getSubmissionVersion(), saved.versionOrZero());
    }

    @Transactional
    public ServiceDtos.WithdrawResponse withdraw(String serviceListingId,
                                                 ServiceDtos.WithdrawRequest request) {
        ServiceListing listing = guard.load(serviceListingId);
        if (listing.getQcStatus() != QcStatus.PENDING_REVIEW) {
            throw new ApiException(ErrorCode.NOT_SUBMITTED,
                    "Only a listing awaiting review can be withdrawn. This one is %s."
                            .formatted(listing.getQcStatus()));
        }
        listing.setQcStatus(null);
        listing.setLifecycle(Lifecycle.DRAFT);
        listing.setSubmittedAt(null);
        ServiceListing saved = repository.save(listing);

        qcReviewService.recordWithdrawal("SERVICE_LISTING", saved.getId(), request.reason());
        audit.record(AuditEvent.SERVICE_WITHDRAWN, "SERVICE_LISTING", saved.getId(),
                Map.of("reason", request.reason() == null ? "" : request.reason()));

        return new ServiceDtos.WithdrawResponse(saved.getId(), saved.getQcStatus(),
                saved.getLifecycle(), saved.versionOrZero(), Instant.now());
    }
}
