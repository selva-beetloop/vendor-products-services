package com.beetloop.catalog.product;

import com.beetloop.catalog.config.CatalogProperties;
import com.beetloop.catalog.product.model.ProductListing;
import com.beetloop.catalog.shared.error.ApiException;
import com.beetloop.catalog.shared.error.ErrorCode;
import com.beetloop.catalog.shared.tenant.TenantContext;
import org.springframework.stereotype.Component;

/** Ownership, editability and lost-update protection, in one place so no route can skip them. */
@Component
public class ProductGuard {

    private final ProductListingRepository repository;
    private final CatalogProperties properties;

    public ProductGuard(ProductListingRepository repository, CatalogProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /** 404 rather than 403 for another vendor's id, so ids are not enumerable. */
    public ProductListing load(String productId) {
        return repository.findByIdAndVendorIdAndDeletedAtIsNull(productId, TenantContext.vendorId())
                .orElseThrow(() -> ApiException.notFound("Product listing " + productId));
    }

    public ProductListing loadEditable(String productId) {
        ProductListing listing = load(productId);
        requireEditable(listing);
        return listing;
    }

    public void requireEditable(ProductListing listing) {
        if (!listing.editable()) {
            throw new ApiException(ErrorCode.NOT_EDITABLE,
                    "This listing is %s. Withdraw it before editing.".formatted(listing.getQcStatus()))
                    .with("qcStatus", listing.getQcStatus())
                    .with("withdrawUrl", "/api/v1/vendor/products/%s/withdraw".formatted(listing.getId()));
        }
    }

    /**
     * If-Match is mandatory on save-all: that endpoint replaces the whole listing, so a stale write
     * is unrecoverable. On a step save it is optional but honoured.
     */
    public void checkIfMatch(ProductListing listing, String ifMatch, boolean required) {
        if (ifMatch == null || ifMatch.isBlank()) {
            if (required && properties.getSave().isRequireIfMatchOnSaveAll()) {
                throw new ApiException(ErrorCode.IF_MATCH_REQUIRED,
                        "PUT /save-all replaces the whole listing. Send If-Match with the ETag from "
                                + "your last read.");
            }
            return;
        }
        String submitted = ifMatch.replace("\"", "").replace("W/", "").trim();
        long current = listing.versionOrZero();
        if (!submitted.equals(String.valueOf(current))) {
            throw new ApiException(ErrorCode.STALE_VERSION,
                    "If-Match \"%s\" does not match the current version %d.".formatted(submitted, current))
                    .with("currentVersion", current)
                    .with("currentEtag", "\"" + current + "\"");
        }
    }

    public static String etag(ProductListing listing) {
        return "\"" + listing.versionOrZero() + "\"";
    }
}
