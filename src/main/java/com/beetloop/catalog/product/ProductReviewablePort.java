package com.beetloop.catalog.product;

import com.beetloop.catalog.product.model.ProductListing;
import com.beetloop.catalog.qc.ReviewableListingPort;
import com.beetloop.catalog.shared.model.Lifecycle;
import com.beetloop.catalog.shared.model.QcStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/** Lets the QC module approve and reject product listings without depending on this package. */
@Component
public class ProductReviewablePort implements ReviewableListingPort {

    private final ProductListingRepository repository;

    public ProductReviewablePort(ProductListingRepository repository) {
        this.repository = repository;
    }

    @Override
    public String entityType() {
        return "PRODUCT_LISTING";
    }

    @Override
    public Optional<Snapshot> find(String entityId) {
        return repository.findById(entityId).map(listing -> new Snapshot(
                listing.getId(), listing.getCode(), listing.getVendorId(),
                listing.getCategoryCode() == null ? null : listing.getCategoryCode().name(),
                String.valueOf(listing.getSearch().get("name")),
                ProductMapper.toResponse(listing),
                listing.versionOrZero()));
    }

    @Override
    public void applyDecision(String entityId, QcStatus qcStatus, Lifecycle lifecycle) {
        ProductListing listing = repository.findById(entityId).orElse(null);
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
