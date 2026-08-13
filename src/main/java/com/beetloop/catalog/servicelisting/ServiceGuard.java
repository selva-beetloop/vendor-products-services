package com.beetloop.catalog.servicelisting;

import com.beetloop.catalog.config.CatalogProperties;
import com.beetloop.catalog.servicelisting.model.ServiceListing;
import com.beetloop.catalog.shared.error.ApiException;
import com.beetloop.catalog.shared.error.ErrorCode;
import com.beetloop.catalog.shared.tenant.TenantContext;
import org.springframework.stereotype.Component;

@Component
public class ServiceGuard {

    private final ServiceListingRepository repository;
    private final CatalogProperties properties;

    public ServiceGuard(ServiceListingRepository repository, CatalogProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public ServiceListing load(String serviceListingId) {
        return repository.findByIdAndVendorIdAndDeletedAtIsNull(serviceListingId, TenantContext.vendorId())
                .orElseThrow(() -> ApiException.notFound("Service listing " + serviceListingId));
    }

    public ServiceListing loadEditable(String serviceListingId) {
        ServiceListing listing = load(serviceListingId);
        if (!listing.editable()) {
            throw new ApiException(ErrorCode.NOT_EDITABLE,
                    "This listing is %s. Withdraw it before editing.".formatted(listing.getQcStatus()))
                    .with("qcStatus", listing.getQcStatus())
                    .with("withdrawUrl", "/api/v1/vendor/services/%s/withdraw".formatted(listing.getId()));
        }
        return listing;
    }

    public void checkIfMatch(ServiceListing listing, String ifMatch, boolean required) {
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

    public static String etag(ServiceListing listing) {
        return "\"" + listing.versionOrZero() + "\"";
    }
}
