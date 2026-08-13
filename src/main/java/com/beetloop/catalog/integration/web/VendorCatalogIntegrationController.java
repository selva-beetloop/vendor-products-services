package com.beetloop.catalog.integration.web;

import com.beetloop.catalog.integration.VendorCatalogPageService;
import com.beetloop.catalog.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Frontend integration surface, kept deliberately separate from /vendor/** .
 *
 * Everything here is READ-ONLY and composed from services that already exist; no endpoint under
 * /vendor/**, /qc/** or /masters/** is modified, replaced or shadowed. The wizard, the save paths
 * and the QC workflow continue to use their own endpoints unchanged.
 */
@Tag(name = "Frontend integration",
        description = "Page-shaped read models for the vendor My Catalog screen")
@RestController
@RequestMapping("/integration/vendor-catalog")
public class VendorCatalogIntegrationController {

    private final VendorCatalogPageService pageService;

    public VendorCatalogIntegrationController(VendorCatalogPageService pageService) {
        this.pageService = pageService;
    }

    @Operation(summary = "Everything the Products tab renders: rows, KPI tiles, facets, category cards")
    @GetMapping("/products")
    public ApiResponse<Map<String, Object>> products(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.of(pageService.productsPage(q, categoryCode, status, page, size));
    }

    @Operation(summary = "Everything the Services tab renders: rows, KPI tiles, facets, category cards")
    @GetMapping("/services")
    public ApiResponse<Map<String, Object>> services(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String configurationStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.of(
                pageService.servicesPage(q, categoryCode, status, configurationStatus, page, size));
    }
}
