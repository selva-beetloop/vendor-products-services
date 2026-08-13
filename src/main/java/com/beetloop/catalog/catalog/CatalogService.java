package com.beetloop.catalog.catalog;

import com.beetloop.catalog.product.ProductListingRepository;
import com.beetloop.catalog.product.model.ProductListing;
import com.beetloop.catalog.servicelisting.ServiceListingRepository;
import com.beetloop.catalog.servicelisting.model.ServiceListing;
import com.beetloop.catalog.shared.api.PageMeta;
import com.beetloop.catalog.shared.api.PagedResponse;
import com.beetloop.catalog.shared.model.Lifecycle;
import com.beetloop.catalog.shared.model.QcStatus;
import com.beetloop.catalog.shared.tenant.TenantContext;
import com.beetloop.catalog.shared.util.Maps;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The two listing tabs, their KPI tiles and their filter facets.
 *
 * Reads only the denormalised `search` sub-document, which is why one query serves eleven columns
 * across five product categories with different data shapes.
 */
@Service
public class CatalogService {

    private final ProductListingRepository products;
    private final ServiceListingRepository services;

    public CatalogService(ProductListingRepository products, ServiceListingRepository services) {
        this.products = products;
        this.services = services;
    }

    // ------------------------------------------------------------------ KPI tiles

    public Map<String, Object> summary(String tab) {
        String vendorId = TenantContext.vendorId();
        boolean isProducts = !"SERVICES".equalsIgnoreCase(tab);

        long total = isProducts ? products.countByVendorIdAndDeletedAtIsNull(vendorId)
                : services.countByVendorIdAndDeletedAtIsNull(vendorId);
        long published = isProducts
                ? products.countByVendorIdAndLifecycleAndDeletedAtIsNull(vendorId, Lifecycle.PUBLISHED)
                : services.countByVendorIdAndLifecycleAndDeletedAtIsNull(vendorId, Lifecycle.PUBLISHED);
        long draft = isProducts ? products.countByVendorIdAndQcStatusIsNullAndDeletedAtIsNull(vendorId)
                : services.countByVendorIdAndQcStatusIsNullAndDeletedAtIsNull(vendorId);
        List<QcStatus> pending = List.of(QcStatus.PENDING_REVIEW, QcStatus.IN_REVIEW);
        long pendingQc = isProducts
                ? products.countByVendorIdAndQcStatusInAndDeletedAtIsNull(vendorId, pending)
                : services.countByVendorIdAndQcStatusInAndDeletedAtIsNull(vendorId, pending);

        Instant windowStart = Instant.now().minus(Duration.ofDays(30));
        long recent = isProducts
                ? products.findByVendorIdAndDeletedAtIsNull(vendorId).stream()
                        .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(windowStart)).count()
                : services.findByVendorIdAndDeletedAtIsNull(vendorId).stream()
                        .filter(s -> s.getCreatedAt() != null && s.getCreatedAt().isAfter(windowStart)).count();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tab", isProducts ? "PRODUCTS" : "SERVICES");
        payload.put("totalListings", tile(total, deltaPct(total, recent), "30d", null));
        payload.put("published", tile(published, null, null, sharePct(published, total)));
        payload.put("draft", tile(draft, null, null, sharePct(draft, total)));
        payload.put("pendingQc", tile(pendingQc, null, null, sharePct(pendingQc, total)));
        payload.put("rfqsReceived", tile(0, null, "30d", null));
        payload.put("lastSavedAt", lastSavedAt(isProducts, vendorId));
        return payload;
    }

    private Map<String, Object> tile(long value, Integer deltaPct, String window, Integer sharePct) {
        Map<String, Object> tile = new LinkedHashMap<>();
        tile.put("value", value);
        if (deltaPct != null) {
            tile.put("deltaPct", deltaPct);
        }
        if (window != null) {
            tile.put("window", window);
        }
        if (sharePct != null) {
            tile.put("sharePct", sharePct);
        }
        return tile;
    }

    private Integer sharePct(long part, long total) {
        return total == 0 ? 0 : (int) Math.round(part * 100.0 / total);
    }

    private Integer deltaPct(long total, long recent) {
        long previous = total - recent;
        return previous <= 0 ? null : (int) Math.round(recent * 100.0 / previous);
    }

    private Instant lastSavedAt(boolean isProducts, String vendorId) {
        return isProducts
                ? products.findByVendorIdAndDeletedAtIsNull(vendorId).stream()
                        .map(ProductListing::getUpdatedAt).filter(java.util.Objects::nonNull)
                        .max(Comparator.naturalOrder()).orElse(null)
                : services.findByVendorIdAndDeletedAtIsNull(vendorId).stream()
                        .map(ServiceListing::getUpdatedAt).filter(java.util.Objects::nonNull)
                        .max(Comparator.naturalOrder()).orElse(null);
    }

    // ------------------------------------------------------------------ grids

    public PagedResponse<Map<String, Object>> listProducts(CatalogQuery query) {
        List<ProductListing> all = products.findByVendorIdAndDeletedAtIsNull(TenantContext.vendorId());
        List<ProductListing> filtered = all.stream()
                .filter(p -> query.categoryCode() == null
                        || query.categoryCode().equals(String.valueOf(p.getCategoryCode())))
                .filter(p -> query.status() == null || query.status().equals(String.valueOf(p.getLifecycle())))
                .filter(p -> matchesText(p.getSearch(), query.q()))
                .sorted(Comparator.comparing(ProductListing::getUpdatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<Map<String, Object>> rows = page(filtered, query).stream().map(this::productRow).toList();
        Map<String, Object> meta = Map.of(
                "displayText", "Showing %d to %d of %d products".formatted(
                        filtered.isEmpty() ? 0 : query.page() * query.size() + 1,
                        Math.min((query.page() + 1) * query.size(), filtered.size()), filtered.size()),
                "pageSizeOptions", List.of(5, 10, 25, 50));
        return PagedResponse.of(rows, PageMeta.of(query.page(), query.size(), filtered.size()), meta);
    }

    public PagedResponse<Map<String, Object>> listServices(CatalogQuery query) {
        List<ServiceListing> all = services.findByVendorIdAndDeletedAtIsNull(TenantContext.vendorId());
        List<ServiceListing> filtered = all.stream()
                .filter(s -> query.categoryCode() == null
                        || query.categoryCode().equals(String.valueOf(s.getCategoryCode())))
                .filter(s -> query.status() == null || query.status().equals(String.valueOf(s.getLifecycle())))
                .filter(s -> query.configurationStatus() == null
                        || query.configurationStatus().equals(s.getSearch().get("configurationStatus")))
                .filter(s -> matchesText(s.getSearch(), query.q()))
                .sorted(Comparator.comparing(ServiceListing::getUpdatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<Map<String, Object>> rows = page(filtered, query).stream().map(this::serviceRow).toList();
        return PagedResponse.of(rows, PageMeta.of(query.page(), query.size(), filtered.size()), null);
    }

    /** Facet counts, because the UI renders "Materials (2)" not just "Materials". */
    public Map<String, Object> filters(String tab) {
        boolean isProducts = !"SERVICES".equalsIgnoreCase(tab);
        String vendorId = TenantContext.vendorId();
        Map<String, Object> payload = new LinkedHashMap<>();

        if (isProducts) {
            List<ProductListing> all = products.findByVendorIdAndDeletedAtIsNull(vendorId);
            payload.put("category", facet(all.stream()
                    .map(p -> String.valueOf(p.getCategoryCode())).toList(), all.size()));
            payload.put("functionalRole", facet(all.stream()
                    .map(p -> String.valueOf(p.getSearch().get("functionalRolePrimary"))).toList(), null));
            payload.put("status", facet(all.stream()
                    .map(p -> String.valueOf(p.getLifecycle())).toList(), null));
        } else {
            List<ServiceListing> all = services.findByVendorIdAndDeletedAtIsNull(vendorId);
            payload.put("category", facet(all.stream()
                    .map(s -> String.valueOf(s.getCategoryCode())).toList(), all.size()));
            payload.put("configurationStatus", facet(all.stream()
                    .map(s -> String.valueOf(s.getSearch().get("configurationStatus"))).toList(), null));
            payload.put("status", facet(all.stream()
                    .map(s -> String.valueOf(s.getLifecycle())).toList(), null));
        }
        payload.put("moreFilters",
                List.of("origin", "sampleAvailability", "certification", "moqRange", "priceRange"));
        return payload;
    }

    private Map<String, Object> facet(List<String> values, Integer allCount) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        values.stream().filter(v -> v != null && !"null".equals(v))
                .forEach(v -> counts.merge(v, 1, Integer::sum));
        List<Map<String, Object>> options = new ArrayList<>();
        if (allCount != null) {
            options.add(Map.of("code", "ALL", "label", "All", "count", allCount));
        }
        counts.forEach((code, count) -> options.add(Map.of("code", code, "label", code, "count", count)));
        return Map.of("options", options);
    }

    private <T> List<T> page(List<T> source, CatalogQuery query) {
        int from = Math.min(query.page() * query.size(), source.size());
        int to = Math.min(from + query.size(), source.size());
        return source.subList(from, to);
    }

    private boolean matchesText(Map<String, Object> search, String q) {
        if (q == null || q.isBlank()) {
            return true;
        }
        String needle = q.toLowerCase();
        String name = Maps.str(search, "name");
        String sku = Maps.str(search, "skuCode");
        if (name != null && name.toLowerCase().contains(needle)) {
            return true;
        }
        if (sku != null && sku.toLowerCase().contains(needle)) {
            return true;
        }
        List<Object> keywords = Maps.asList(search.get("keywords"));
        return keywords != null && keywords.stream()
                .anyMatch(k -> String.valueOf(k).toLowerCase().contains(needle));
    }

    private Map<String, Object> productRow(ProductListing listing) {
        Map<String, Object> row = new LinkedHashMap<>(listing.getSearch());
        row.put("id", listing.getId());
        row.put("code", listing.getCode());
        row.put("categoryCode", listing.getCategoryCode());
        row.put("status", listing.getLifecycle());
        row.put("qcStatus", listing.getQcStatus());
        row.put("verified", listing.getQcStatus() == QcStatus.APPROVED);
        row.put("variantCount", listing.getVariants().size());
        row.put("updatedAt", listing.getUpdatedAt());
        row.put("actions", List.of("VIEW", "EDIT", "DUPLICATE", "UPDATE_PRICE", "ARCHIVE", "DELETE"));
        return row;
    }

    private Map<String, Object> serviceRow(ServiceListing listing) {
        Map<String, Object> row = new LinkedHashMap<>(listing.getSearch());
        row.put("id", listing.getId());
        row.put("code", listing.getCode());
        row.put("categoryCode", listing.getCategoryCode());
        row.put("status", listing.getLifecycle());
        row.put("qcStatus", listing.getQcStatus());
        row.put("updatedAt", listing.getUpdatedAt());
        return row;
    }

    // ------------------------------------------------------------------ bulk

    public Map<String, Object> bulkAction(String tab, String action, List<String> ids) {
        boolean isProducts = !"SERVICES".equalsIgnoreCase(tab);
        List<Map<String, Object>> results = new ArrayList<>();
        int succeeded = 0;

        for (String id : ids == null ? List.<String>of() : ids) {
            try {
                if (isProducts) {
                    ProductListing listing = products
                            .findByIdAndVendorIdAndDeletedAtIsNull(id, TenantContext.vendorId())
                            .orElseThrow();
                    applyBulk(listing.getQcStatus(), action, listing::setLifecycle, listing::setDeletedAt);
                    products.save(listing);
                } else {
                    ServiceListing listing = services
                            .findByIdAndVendorIdAndDeletedAtIsNull(id, TenantContext.vendorId())
                            .orElseThrow();
                    applyBulk(listing.getQcStatus(), action, listing::setLifecycle, listing::setDeletedAt);
                    services.save(listing);
                }
                succeeded++;
                results.add(Map.of("id", id, "status", "OK"));
            } catch (RuntimeException e) {
                results.add(Map.of("id", id, "status", "FAILED",
                        "message", e.getMessage() == null ? "Not applicable" : e.getMessage()));
            }
        }
        return Map.of("action", action, "requested", results.size(), "succeeded", succeeded,
                "failed", results.size() - succeeded, "results", results);
    }

    private void applyBulk(QcStatus qcStatus, String action,
                           java.util.function.Consumer<Lifecycle> lifecycleSetter,
                           java.util.function.Consumer<Instant> deletedSetter) {
        if (qcStatus == QcStatus.PENDING_REVIEW || qcStatus == QcStatus.IN_REVIEW) {
            throw new IllegalStateException("Listing is " + qcStatus + " and cannot be modified in bulk.");
        }
        switch (action.toUpperCase()) {
            case "ARCHIVE" -> lifecycleSetter.accept(Lifecycle.ARCHIVED);
            case "MARK_AS_DRAFT" -> lifecycleSetter.accept(Lifecycle.DRAFT);
            case "PUBLISH" -> {
                if (qcStatus != QcStatus.APPROVED) {
                    throw new IllegalStateException("Only a QC-approved listing can be published.");
                }
                lifecycleSetter.accept(Lifecycle.PUBLISHED);
            }
            case "DELETE" -> deletedSetter.accept(Instant.now());
            default -> throw new IllegalArgumentException("Unsupported action " + action);
        }
    }

    public record CatalogQuery(String q, String categoryCode, String status, String configurationStatus,
                               int page, int size) {
    }
}
