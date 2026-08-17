package com.beetloop.vendorproducts.controller;

import com.beetloop.vendorproducts.domain.ProductCategory;
import com.beetloop.vendorproducts.domain.ProductStatus;
import com.beetloop.vendorproducts.dto.ApiError;
import com.beetloop.vendorproducts.dto.CreateProductRequest;
import com.beetloop.vendorproducts.dto.IdentityStepRequest;
import com.beetloop.vendorproducts.dto.OverallSaveRequest;
import com.beetloop.vendorproducts.dto.PageResponse;
import com.beetloop.vendorproducts.dto.ProductResponse;
import com.beetloop.vendorproducts.dto.ProductSummaryResponse;
import com.beetloop.vendorproducts.dto.QcDecisionRequest;
import com.beetloop.vendorproducts.dto.RoleStepRequest;
import com.beetloop.vendorproducts.dto.VariantRequest;
import com.beetloop.vendorproducts.dto.VariantResponse;
import com.beetloop.vendorproducts.service.VendorProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * All product-wizard endpoints, mounted at
 * {@code http://localhost:8086/vendor-products/api/vendor/products}.
 */
@RestController
@RequestMapping("/api/vendor/products")
@Tag(name = "Vendor Products", description = "Add Product wizard: step-based saves, overall save and QC.")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation failed",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Product or variant not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Action not allowed from the current status",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
})
public class VendorProductController {

    private final VendorProductService service;

    public VendorProductController(VendorProductService service) {
        this.service = service;
    }

    // ---------------------------------------------------------------- create

    @PostMapping
    @Operation(summary = "Create a draft product",
            description = "Called when the vendor picks a category card on the 'List a Product' screen. "
                    + "Returns the id every later step uses.")
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody CreateProductRequest request,
            @RequestHeader(value = "X-VENDOR-ID", required = false) String vendorId,
            @RequestHeader(value = "X-USER-ID", required = false) String userId) {
        ProductResponse created = service.create(request, vendorId, userId);
        return ResponseEntity.created(URI.create("/api/vendor/products/" + created.id())).body(created);
    }

    // ----------------------------------------------------------------- reads

    @GetMapping
    @Operation(summary = "List products",
            description = "Backs the catalog table: free-text search over name/SKU/category, the category "
                    + "chip filter, the status filter and pagination.")
    public PageResponse<ProductSummaryResponse> list(
            @RequestHeader(value = "X-VENDOR-ID", required = false) String vendorId,
            @Parameter(description = "raw-materials | processing-machinery | finished-goods | "
                    + "packaging-materials | packaging-machinery — also accepts the catalog groupId")
            @RequestParam(required = false) ProductCategory category,
            @Parameter(description = "DRAFT | SUBMITTED_FOR_QC | APPROVED | … — also accepts a StatusKind "
                    + "such as qc-pending")
            @RequestParam(required = false) ProductStatus status,
            @Parameter(description = "Matches name, SKU or category")
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "property,asc|desc — e.g. name,asc")
            @RequestParam(required = false) String sort) {
        return service.list(vendorId, category, status, search, page, size, sort);
    }

    @GetMapping("/qc-review")
    @Operation(summary = "QC review queue",
            description = "Products awaiting review (SUBMITTED_FOR_QC or PENDING_REVIEW).")
    public PageResponse<ProductSummaryResponse> qcQueue(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {
        return service.qcQueue(search, page, size, sort);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one product with full nested detail",
            description = "Returns identity, role and every variant with all five sub-steps — enough to "
                    + "rehydrate the whole wizard.")
    public ProductResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    // ------------------------------------------------- step-based (A) saves

    @PutMapping("/{id}/identity")
    @Operation(summary = "Save Step 1 — Product / Machine Identity",
            description = "Validates only this step. Send `draft: true` to store partial progress "
                    + "without required-field checks.")
    public ProductResponse saveIdentity(@PathVariable UUID id,
                                        @Valid @RequestBody IdentityStepRequest request) {
        return service.saveIdentity(id, request);
    }

    public record RepointCommercialRequest(String commercialMasterId, String commercialMasterCode) {
    }

    @PutMapping("/{id}/commercial-master")
    @Operation(summary = "Re-point this T3 at another T2 after a grade-defining branch")
    public ProductResponse repoint(@PathVariable UUID id,
                                   @RequestBody RepointCommercialRequest request) {
        String target = request.commercialMasterId() != null
                ? request.commercialMasterId() : request.commercialMasterCode();
        return service.repointCommercial(id, target);
    }

    @PutMapping("/{id}/role")
    @Operation(summary = "Save Step 2 — Your Role & Supply Information",
            description = "Validates only this step and preserves the identity saved earlier.")
    public ProductResponse saveRole(@PathVariable UUID id,
                                    @Valid @RequestBody RoleStepRequest request) {
        return service.saveRole(id, request);
    }

    @GetMapping("/{id}/variants")
    @Operation(summary = "List variants for a product")
    public List<VariantResponse> listVariants(@PathVariable UUID id) {
        return service.listVariants(id);
    }

    @PostMapping("/{id}/variants")
    @Operation(summary = "Add a variant",
            description = "Accepts all five Add Variant sub-steps in one payload — what the wizard sends "
                    + "when the vendor clicks 'Add Variant'.")
    public ResponseEntity<VariantResponse> addVariant(@PathVariable UUID id,
                                                      @Valid @RequestBody VariantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addVariant(id, request));
    }

    @PutMapping("/{id}/variants/{variantId}")
    @Operation(summary = "Update a variant")
    public VariantResponse updateVariant(@PathVariable UUID id,
                                         @PathVariable UUID variantId,
                                         @Valid @RequestBody VariantRequest request) {
        return service.updateVariant(id, variantId, request);
    }

    @PutMapping("/{id}/variants/{variantId}/{section}")
    @Operation(summary = "Save one variant sub-step",
            description = "`section` is one of `details`, `technical-specifications`, `commercial-pricing`, "
                    + "`compliance-documents`, `search-marketplace`. Lets the vendor leave the Add Variant "
                    + "wizard mid-way without losing entered data.")
    public VariantResponse saveVariantSection(@PathVariable UUID id,
                                              @PathVariable UUID variantId,
                                              @PathVariable String section,
                                              @Valid @RequestBody VariantRequest request) {
        return service.saveVariantSection(id, variantId, section, request);
    }

    @DeleteMapping("/{id}/variants/{variantId}")
    @Operation(summary = "Delete a variant")
    public ResponseEntity<Void> deleteVariant(@PathVariable UUID id, @PathVariable UUID variantId) {
        service.deleteVariant(id, variantId);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------- overall (B) save + QC

    @PostMapping("/{id}/save")
    @Operation(summary = "Overall save",
            description = "Writes Product Identity + Your Role + Variants in a single transaction. "
                    + "Sections omitted keep whatever the step-based saves stored. Set `submitForQc: true` "
                    + "to submit in the same call.")
    public ProductResponse saveAll(@PathVariable UUID id,
                                   @Valid @RequestBody OverallSaveRequest request) {
        return service.saveAll(id, request);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit for QC",
            description = "Validates the fully-assembled product and moves it to SUBMITTED_FOR_QC. "
                    + "Backs the 'Submit for QC' button on Review & Submit.")
    public ProductResponse submit(@PathVariable UUID id) {
        return service.submitForQc(id);
    }

    @PutMapping("/{id}/qc-decision")
    @Operation(summary = "Record a QC decision",
            description = "APPROVE / REJECT / QUERY / PUBLISH. REJECT and QUERY require remarks and return "
                    + "the product to an editable state.")
    public ProductResponse qcDecision(@PathVariable UUID id,
                                      @Valid @RequestBody QcDecisionRequest request) {
        return service.qcDecision(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
