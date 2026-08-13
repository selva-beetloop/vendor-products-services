package com.beetloop.catalog.integration;

import com.beetloop.catalog.catalog.CatalogService;
import com.beetloop.catalog.masters.MasterCategory;
import com.beetloop.catalog.masters.MasterCategoryRepository;
import com.beetloop.catalog.product.ProductListingRepository;
import com.beetloop.catalog.product.model.ProductListing;
import com.beetloop.catalog.servicelisting.ServiceListingRepository;
import com.beetloop.catalog.servicelisting.model.ServiceListing;
import com.beetloop.catalog.shared.api.PagedResponse;
import com.beetloop.catalog.shared.model.ListingType;
import com.beetloop.catalog.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read-only composition layer for the vendor "My Catalog" screen.
 *
 * The screen needs the grid, the KPI tiles and the filter facets together; issuing three round
 * trips from the browser for one page render is wasteful and makes the loading states race. This
 * assembles them from the EXISTING CatalogService — it adds no new persistence, no new write path
 * and changes nothing about the endpoints already in place.
 */
@Service
public class VendorCatalogPageService {

    private final CatalogService catalogService;
    private final MasterCategoryRepository categories;
    private final ProductListingRepository products;
    private final ServiceListingRepository services;

    public VendorCatalogPageService(CatalogService catalogService,
                                    MasterCategoryRepository categories,
                                    ProductListingRepository products,
                                    ServiceListingRepository services) {
        this.catalogService = catalogService;
        this.categories = categories;
        this.products = products;
        this.services = services;
    }

    public Map<String, Object> productsPage(String q, String categoryCode, String status,
                                            int page, int size) {
        PagedResponse<Map<String, Object>> rows = catalogService.listProducts(
                new CatalogService.CatalogQuery(q, categoryCode, status, null, page, size));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tab", "PRODUCTS");
        payload.put("rows", withProductDerived(rows.data()));
        payload.put("page", rows.page());
        payload.put("meta", rows.meta());
        payload.put("summary", catalogService.summary("PRODUCTS"));
        payload.put("filters", catalogService.filters("PRODUCTS"));
        payload.put("categories", categoryCards(ListingType.PRODUCT));
        return payload;
    }

    public Map<String, Object> servicesPage(String q, String categoryCode, String status,
                                            String configurationStatus, int page, int size) {
        PagedResponse<Map<String, Object>> rows = catalogService.listServices(
                new CatalogService.CatalogQuery(q, categoryCode, status, configurationStatus,
                        page, size));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tab", "SERVICES");
        payload.put("rows", withServiceDerived(rows.data()));
        payload.put("page", rows.page());
        payload.put("meta", rows.meta());
        payload.put("summary", catalogService.summary("SERVICES"));
        payload.put("filters", catalogService.filters("SERVICES"));
        payload.put("categories", categoryCards(ListingType.SERVICE));
        return payload;
    }

    /**
     * The grid renders a Completion column and a document-coverage cell, and `search` — which is
     * all {@link CatalogService} projects — carries neither; both live in `derived`, computed by
     * the recalculators on every save. Copying the four figures onto the row here means the page
     * shows the real numbers instead of the client inventing them from the lifecycle.
     *
     * Read-only and additive: the row map is copied, `CatalogService` is not touched, and the
     * /vendor/** grid endpoint keeps returning exactly what it returned before.
     */
    private List<Map<String, Object>> withProductDerived(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return rows;
        }
        Map<String, ProductListing> byId = products
                .findByVendorIdAndDeletedAtIsNull(TenantContext.vendorId()).stream()
                .collect(Collectors.toMap(ProductListing::getId, Function.identity(),
                        (a, b) -> a, LinkedHashMap::new));

        return rows.stream().map(row -> {
            Map<String, Object> enriched = new LinkedHashMap<>(row);
            ProductListing listing = byId.get(String.valueOf(row.get("id")));
            if (listing != null) {
                Map<String, Object> derived = listing.getDerived();
                copy(enriched, derived, "completionPercent");
                copy(enriched, derived, "documentCount");
                copy(enriched, derived, "expiredDocumentCount");
                enriched.put("currentStep", listing.getCurrentStep());
            }
            return enriched;
        }).toList();
    }

    /** The services mirror: configuration counters and completion, same derivation, same rules. */
    private List<Map<String, Object>> withServiceDerived(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return rows;
        }
        Map<String, ServiceListing> byId = services
                .findByVendorIdAndDeletedAtIsNull(TenantContext.vendorId()).stream()
                .collect(Collectors.toMap(ServiceListing::getId, Function.identity(),
                        (a, b) -> a, LinkedHashMap::new));

        return rows.stream().map(row -> {
            Map<String, Object> enriched = new LinkedHashMap<>(row);
            ServiceListing listing = byId.get(String.valueOf(row.get("id")));
            if (listing != null) {
                Map<String, Object> derived = listing.getDerived();
                copy(enriched, derived, "completionPercent");
                copy(enriched, derived, "configurationCount");
                copy(enriched, derived, "configuredCount");
                copy(enriched, derived, "accreditationCount");
                copy(enriched, derived, "certificationCount");
                copy(enriched, derived, "supportDocCount");
                copy(enriched, derived, "expiredDocumentCount");
                enriched.put("currentStep", listing.getCurrentStep());
            }
            return enriched;
        }).toList();
    }

    private void copy(Map<String, Object> target, Map<String, Object> derived, String key) {
        if (derived != null && derived.get(key) != null) {
            target.put(key, derived.get(key));
        }
    }

    /** The cards on "List a Product" / "List a Service", so the chooser renders from real data. */
    private List<Map<String, Object>> categoryCards(ListingType type) {
        return categories.findByTypeOrderByOrderAsc(type).stream()
                .filter(MasterCategory::isLive)
                .map(c -> {
                    Map<String, Object> card = new LinkedHashMap<>();
                    card.put("code", c.getCode());
                    card.put("title", c.getTitle());
                    card.put("description", c.getDescription());
                    card.put("examples", c.getExamples());
                    card.put("buttonLabel", c.getButtonLabel());
                    card.put("escapeHatchLabel", c.getEscapeHatchLabel());
                    card.put("commonFor", c.getCommonFor());
                    card.put("outerSteps", c.getOuterSteps());
                    card.put("configurationSubSteps", c.getConfigurationSubSteps());
                    return card;
                })
                .toList();
    }
}
