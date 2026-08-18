package com.beetloop.vendorproducts.controller;

import com.beetloop.vendorproducts.domain.ProductCategory;
import com.beetloop.vendorproducts.dto.PageResponse;
import com.beetloop.vendorproducts.dto.ProductResponse;
import com.beetloop.vendorproducts.dto.ProductSummaryResponse;
import com.beetloop.vendorproducts.service.VendorProductService;
import com.beetloop.vendorproducts.services.domain.ServiceCategory;
import com.beetloop.vendorproducts.services.dto.ServiceDtos;
import com.beetloop.vendorproducts.services.service.VendorServiceCatalogService;
import com.beetloop.vendorproducts.storage.FileStorage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Public published catalogue for Buyer marketplace. Only {@code PUBLISHED} T3
 * listings are returned — drafts, QC, query and rejected records are 404.
 */
@RestController
@RequestMapping("/api/marketplace")
@Tag(name = "Marketplace", description = "Published-only product and service catalogue for buyers.")
public class MarketplaceController {

    private final VendorProductService products;
    private final VendorServiceCatalogService services;
    private final FileStorage storage;

    public MarketplaceController(VendorProductService products,
                                 VendorServiceCatalogService services,
                                 FileStorage storage) {
        this.products = products;
        this.services = services;
        this.storage = storage;
    }

    @GetMapping("/products")
    @Operation(summary = "List published products")
    public PageResponse<ProductSummaryResponse> listProducts(
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String sort) {
        return products.listPublished(category, search, page, size, sort);
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "Get one published product (T3 + attached master ids)")
    public ProductResponse getProduct(@PathVariable UUID id) {
        return products.getPublished(id);
    }

    @GetMapping("/services")
    @Operation(summary = "List published services")
    public PageResponse<ServiceDtos.ServiceSummaryResponse> listServices(
            @RequestParam(required = false) ServiceCategory category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String sort) {
        return services.listPublished(category, search, page, size, sort);
    }

    @GetMapping("/services/{id}")
    @Operation(summary = "Get one published service item")
    public ServiceDtos.ServiceResponse getService(@PathVariable UUID id) {
        return services.getPublished(id);
    }

    @GetMapping("/files/{id}")
    @Operation(summary = "Download a catalogue file referenced by a published listing")
    public ResponseEntity<Resource> download(@PathVariable String id) {
        FileStorage.StoredUpload stored = storage.metadata(id);
        Resource resource = storage.content(id);
        String contentType = stored.contentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : stored.contentType();
        String fileName = stored.fileName() == null ? id : stored.fileName();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(resource);
    }
}
