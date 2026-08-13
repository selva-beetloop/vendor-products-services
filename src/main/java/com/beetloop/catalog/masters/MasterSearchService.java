package com.beetloop.catalog.masters;

import com.beetloop.catalog.shared.api.PageMeta;
import com.beetloop.catalog.shared.api.PagedResponse;
import com.beetloop.catalog.shared.error.ApiException;
import com.beetloop.catalog.shared.model.ListingType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Backs the per-category master search screen, including its facet counts and escape hatch. */
@Service
public class MasterSearchService {

    private final MasterCatalogRepository repository;
    private final MasterCategoryRepository categories;

    public MasterSearchService(MasterCatalogRepository repository, MasterCategoryRepository categories) {
        this.repository = repository;
        this.categories = categories;
    }

    public record SearchRequest(String categoryCode, String q, Map<String, List<String>> facets,
                                String sort, Integer page, Integer size) {
    }

    public PagedResponse<MasterCatalogEntry> search(ListingType type, SearchRequest request) {
        List<MasterCatalogEntry> all = repository.findByTypeAndCategoryCode(type, request.categoryCode());

        List<MasterCatalogEntry> filtered = all.stream()
                .filter(e -> matchesText(e, request.q()))
                .filter(e -> matchesFacets(e, request.facets()))
                .toList();

        int page = request.page() == null ? 0 : request.page();
        int size = request.size() == null ? 10 : request.size();
        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());

        MasterCategory category = categories.findByCode(request.categoryCode())
                .orElseThrow(() -> ApiException.notFound("Category " + request.categoryCode()));

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("resultCountLabel", "%d %s Found".formatted(filtered.size(),
                type == ListingType.PRODUCT ? "Products" : "Services"));
        meta.put("facetCounts", facetCounts(all, category.getSearchFacets()));
        meta.put("escapeHatch", Map.of(
                "label", category.getEscapeHatchLabel() == null ? "Add New" : category.getEscapeHatchLabel(),
                "url", "/vendor/%s?categoryCode=%s&entryPath=REQUEST_NEW"
                        .formatted(type == ListingType.PRODUCT ? "products" : "services",
                                request.categoryCode())));
        meta.put("pageSizeOptions", List.of(10, 20, 50));

        return PagedResponse.of(filtered.subList(from, to),
                PageMeta.of(page, size, filtered.size()), meta);
    }

    private boolean matchesText(MasterCatalogEntry entry, String q) {
        if (q == null || q.isBlank()) {
            return true;
        }
        String needle = q.toLowerCase();
        if (entry.getName() != null && entry.getName().toLowerCase().contains(needle)) {
            return true;
        }
        return entry.getSynonyms() != null && entry.getSynonyms().stream()
                .anyMatch(s -> s.toLowerCase().contains(needle));
    }

    private boolean matchesFacets(MasterCatalogEntry entry, Map<String, List<String>> facets) {
        if (facets == null || facets.isEmpty()) {
            return true;
        }
        Map<String, String> entryFacets = entry.getFacets() == null ? Map.of() : entry.getFacets();
        for (Map.Entry<String, List<String>> facet : facets.entrySet()) {
            if (facet.getValue() == null || facet.getValue().isEmpty()) {
                continue;
            }
            if (!facet.getValue().contains(entryFacets.get(facet.getKey()))) {
                return false;
            }
        }
        return true;
    }

    private Map<String, Object> facetCounts(List<MasterCatalogEntry> entries, List<String> facetKeys) {
        Map<String, Object> counts = new LinkedHashMap<>();
        if (facetKeys == null) {
            return counts;
        }
        for (String key : facetKeys) {
            Map<String, Integer> byValue = new LinkedHashMap<>();
            for (MasterCatalogEntry entry : entries) {
                String value = entry.getFacets() == null ? null : entry.getFacets().get(key);
                if (value != null) {
                    byValue.merge(value, 1, Integer::sum);
                }
            }
            List<Map<String, Object>> options = new ArrayList<>();
            byValue.forEach((code, count) -> options.add(Map.of("code", code, "count", count)));
            counts.put(key, options);
        }
        return counts;
    }
}
