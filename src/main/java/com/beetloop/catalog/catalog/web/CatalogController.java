package com.beetloop.catalog.catalog.web;

import com.beetloop.catalog.catalog.CatalogService;
import com.beetloop.catalog.shared.api.ApiResponse;
import com.beetloop.catalog.shared.api.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "Catalog", description = "My Catalog: the two listing tabs, KPI tiles and filter facets")
@RestController
@RequestMapping("/vendor/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/products")
    public PagedResponse<Map<String, Object>> products(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return catalogService.listProducts(
                new CatalogService.CatalogQuery(q, categoryCode, status, null, page, size));
    }

    @GetMapping("/services")
    public PagedResponse<Map<String, Object>> services(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String configurationStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return catalogService.listServices(
                new CatalogService.CatalogQuery(q, categoryCode, status, configurationStatus, page, size));
    }

    @Operation(summary = "KPI tiles - one endpoint serves both tabs")
    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary(@RequestParam(defaultValue = "PRODUCTS") String tab) {
        return ApiResponse.of(catalogService.summary(tab));
    }

    @Operation(summary = "Filter options WITH facet counts")
    @GetMapping("/filters")
    public ApiResponse<Map<String, Object>> filters(@RequestParam(defaultValue = "PRODUCTS") String tab) {
        return ApiResponse.of(catalogService.filters(tab));
    }

    public record BulkRequest(String tab, String action, List<String> ids) {
    }

    @PostMapping("/bulk-action")
    public ApiResponse<Map<String, Object>> bulkAction(@RequestBody BulkRequest request) {
        return ApiResponse.of(catalogService.bulkAction(request.tab(), request.action(), request.ids()));
    }
}
