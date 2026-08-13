package com.beetloop.catalog.product.web;

import com.beetloop.catalog.product.ProductVariantService;
import com.beetloop.catalog.product.dto.ProductDtos;
import com.beetloop.catalog.shared.api.ApiResponse;
import com.beetloop.catalog.shared.api.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(name = "Product variants", description = "Variant CRUD and the stage-scoped section save")
@RestController
@RequestMapping("/vendor/products/{productId}/variants")
public class ProductVariantController {

    private final ProductVariantService variantService;

    public ProductVariantController(ProductVariantService variantService) {
        this.variantService = variantService;
    }

    @Operation(summary = "List variants, with grouping counts for the By Grade / By Pack Size tabs")
    @GetMapping
    public PagedResponse<ProductDtos.VariantResponse> list(
            @PathVariable String productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String variantType,
            @RequestParam(required = false) String groupBy) {
        return variantService.list(productId, page, size, status, variantType, groupBy);
    }

    @Operation(summary = "Create a variant. An empty body is valid - Add Variant opens an empty builder.")
    @PostMapping
    public ResponseEntity<ApiResponse<ProductDtos.VariantResponse>> create(
            @PathVariable String productId,
            @RequestBody(required = false) ProductDtos.CreateVariantRequest request) {
        ProductDtos.VariantResponse variant = variantService.create(productId, request);
        return ResponseEntity
                .created(URI.create("/api/v1/vendor/products/%s/variants/%s"
                        .formatted(productId, variant.variantId())))
                .body(ApiResponse.of(variant));
    }

    @GetMapping("/{variantId}")
    public ApiResponse<ProductDtos.VariantResponse> get(@PathVariable String productId,
                                                        @PathVariable String variantId) {
        return ApiResponse.of(variantService.get(productId, variantId));
    }

    @Operation(summary = "SECTION SAVE: one variant stage, siblings untouched (fires on Next)")
    @PutMapping("/{variantId}/sections/{sectionKey}")
    public ApiResponse<ProductDtos.VariantSectionSaveResponse> saveSection(
            @PathVariable String productId,
            @PathVariable String variantId,
            @PathVariable String sectionKey,
            @RequestBody ProductDtos.VariantSectionSaveRequest request) {
        return ApiResponse.of(variantService.saveSection(productId, variantId, sectionKey, request));
    }

    @Operation(summary = "Whole-variant save: every stage in one call (fires on the final Add Variant)")
    @PutMapping("/{variantId}")
    public ApiResponse<ProductDtos.VariantWholeSaveResponse> saveWhole(
            @PathVariable String productId,
            @PathVariable String variantId,
            @RequestBody ProductDtos.VariantWholeSaveRequest request) {
        return ApiResponse.of(variantService.saveWhole(productId, variantId, request));
    }

    @DeleteMapping("/{variantId}")
    public ResponseEntity<Void> delete(@PathVariable String productId, @PathVariable String variantId) {
        variantService.delete(productId, variantId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Publish / Archive / Delete / Mark as Draft, reported per id")
    @PostMapping("/bulk-action")
    public ApiResponse<ProductDtos.BulkActionResponse> bulkAction(
            @PathVariable String productId,
            @Valid @RequestBody ProductDtos.BulkActionRequest request) {
        return ApiResponse.of(variantService.bulkAction(productId, request));
    }
}
