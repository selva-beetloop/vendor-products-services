package com.beetloop.catalog.servicelisting;

import com.beetloop.catalog.qc.ReviewableListingPort;
import com.beetloop.catalog.servicelisting.model.ServiceListing;
import com.beetloop.catalog.shared.model.Lifecycle;
import com.beetloop.catalog.shared.model.QcStatus;
import com.beetloop.catalog.template.TemplateService;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class ServiceReviewablePort implements ReviewableListingPort {

    private final ServiceListingRepository repository;
    private final TemplateService templates;

    public ServiceReviewablePort(ServiceListingRepository repository, TemplateService templates) {
        this.repository = repository;
        this.templates = templates;
    }

    @Override
    public String entityType() {
        return "SERVICE_LISTING";
    }

    @Override
    public Optional<Snapshot> find(String entityId) {
        return repository.findById(entityId).map(listing -> new Snapshot(
                listing.getId(), listing.getCode(), listing.getVendorId(),
                listing.getCategoryCode() == null ? null : listing.getCategoryCode().name(),
                String.valueOf(listing.getSearch().get("name")),
                ServiceMapper.toResponse(listing, templates.forListing(
                        listing.getCategoryCode().name(), listing.getTemplateVersion())),
                listing.versionOrZero()));
    }

    @Override
    public void applyDecision(String entityId, QcStatus qcStatus, Lifecycle lifecycle) {
        ServiceListing listing = repository.findById(entityId).orElse(null);
        if (listing == null) {
            return;
        }
        listing.setQcStatus(qcStatus);
        if (lifecycle != null) {
            listing.setLifecycle(lifecycle);
            if (lifecycle == Lifecycle.PUBLISHED) {
                listing.setPublishedAt(Instant.now());
            }
        }
        repository.save(listing);
    }
}
