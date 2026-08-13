package com.beetloop.catalog.product.web;

import com.beetloop.catalog.product.ProductDraftService;
import com.beetloop.catalog.product.ProductGuard;
import com.beetloop.catalog.product.ProductMapper;
import com.beetloop.catalog.product.ProductOverallSaveService;
import com.beetloop.catalog.product.ProductReviewService;
import com.beetloop.catalog.product.ProductStepSaveService;
import com.beetloop.catalog.product.ProductSubmitService;
import com.beetloop.catalog.product.dto.ProductDtos;
import com.beetloop.catalog.product.model.ProductListing;
import com.beetloop.catalog.shared.api.ApiResponse;
import com.beetloop.catalog.shared.idempotency.IdempotencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(name = "Products", description = "Vendor product listings: draft, step-wise save, overall save, submit")
@RestController
@RequestMapping("/vendor/products")
public class ProductController {

    private final ProductDraftService draftService;
    private final ProductStepSaveService stepSaveService;
    private final ProductOverallSaveService overallSaveService;
    private final ProductReviewService reviewService;
    private final ProductSubmitService submitService;
    private final IdempotencyService idempotency;

    public ProductController(ProductDraftService draftService, ProductStepSaveService stepSaveService,
                             ProductOverallSaveService overallSaveService,
                             ProductReviewService reviewService, ProductSubmitService submitService,
                             IdempotencyService idempotency) {
        this.draftService = draftService;
        this.stepSaveService = stepSaveService;
        this.overallSaveService = overallSaveService;
        this.reviewService = reviewService;
        this.submitService = submitService;
        this.idempotency = idempotency;
    }

    // ------------------------------------------------------------------ lifecycle

    @Operation(summary = "Create a draft listing (Add to Catalog, or the Add New Material escape hatch)")
    @PostMapping
    public ResponseEntity<ApiResponse<ProductDtos.ProductResponse>> create(
            @Valid @RequestBody ProductDtos.CreateProductRequest request) {
        ProductListing listing = draftService.create(request);
        return ResponseEntity
                .created(URI.create("/api/v1/vendor/products/" + listing.getId()))
                .eTag(ProductGuard.etag(listing))
                .body(ApiResponse.of(ProductMapper.toResponse(listing)));
    }

    @Operation(summary = "Read the whole listing")
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDtos.ProductResponse>> get(@PathVariable String productId) {
        ProductListing listing = draftService.get(productId);
        return ResponseEntity.ok()
                .eTag(ProductGuard.etag(listing))
                .body(ApiResponse.of(ProductMapper.toResponse(listing)));
    }

    @Operation(summary = "Soft-delete a draft")
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> delete(@PathVariable String productId) {
        draftService.delete(productId);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ STEP-WISE SAVE

    @Operation(summary = "Read one step")
    @GetMapping("/{productId}/steps/{stepKey}")
    public ApiResponse<ProductDtos.StepReadResponse> readStep(@PathVariable String productId,
                                                              @PathVariable String stepKey) {
        return ApiResponse.of(stepSaveService.read(productId, stepKey));
    }

    @Operation(summary = "STEP-WISE SAVE: replace one step, siblings untouched. Never changes qcStatus.")
    @PutMapping("/{productId}/steps/{stepKey}")
    public ResponseEntity<ApiResponse<ProductDtos.StepSaveResponse>> saveStep(
            @PathVariable String productId,
            @PathVariable String stepKey,
            @RequestBody ProductDtos.StepSaveRequest request,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        ProductDtos.StepSaveResponse response = stepSaveService.save(productId, stepKey, request, ifMatch);
        return ResponseEntity.ok().eTag(response.etag()).body(ApiResponse.of(response));
    }

    @Operation(summary = "Dry run: what submit-qc would say about this step. Writes nothing.")
    @PostMapping("/{productId}/steps/{stepKey}/validate")
    public ApiResponse<ProductDtos.ValidateStepResponse> validateStep(
            @PathVariable String productId,
            @PathVariable String stepKey,
            @RequestBody ProductDtos.StepSaveRequest request) {
        return ApiResponse.of(stepSaveService.validate(productId, stepKey, request));
    }

    // ------------------------------------------------------------------ OVERALL SAVE

    @Operation(summary = "OVERALL SAVE (create or update): all four steps and every variant, one "
            + "transaction. Full replace. Still a draft write - this is NOT submit.")
    @PostMapping("/save-all")
    public ResponseEntity<ApiResponse<ProductDtos.SaveAllResponse>> saveAllCreate(
            @Valid @RequestBody ProductDtos.SaveAllRequest request,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        boolean creating = request.productId() == null;
        ProductDtos.SaveAllResponse response = idempotency.execute(idempotencyKey,
                "POST /vendor/products/save-all", request, ProductDtos.SaveAllResponse.class,
                () -> overallSaveService.saveAll(null, request, ifMatch, false));
        ResponseEntity.BodyBuilder builder = creating
                ? ResponseEntity.status(HttpStatus.CREATED)
                        .location(URI.create("/api/v1/vendor/products/" + response.productId()))
                : ResponseEntity.ok();
        return builder.eTag(response.etag()).body(ApiResponse.of(response));
    }

    @Operation(summary = "OVERALL SAVE (update). If-Match is REQUIRED: this replaces the whole listing.")
    @PutMapping("/{productId}/save-all")
    public ResponseEntity<ApiResponse<ProductDtos.SaveAllResponse>> saveAllUpdate(
            @PathVariable String productId,
            @Valid @RequestBody ProductDtos.SaveAllRequest request,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        ProductDtos.SaveAllResponse response = idempotency.execute(idempotencyKey,
                "PUT /vendor/products/{id}/save-all", request, ProductDtos.SaveAllResponse.class,
                () -> overallSaveService.saveAll(productId, request, ifMatch, true));
        return ResponseEntity.ok().eTag(response.etag()).body(ApiResponse.of(response));
    }

    // ------------------------------------------------------------------ review & submit

    @Operation(summary = "Server-computed review projection with REAL completeness flags")
    @GetMapping("/{productId}/review")
    public ApiResponse<ProductDtos.ReviewResponse> review(@PathVariable String productId) {
        return ApiResponse.of(reviewService.review(productId));
    }

    @Operation(summary = "Submit for QC - the only gate. Re-validates everything from scratch.")
    @PostMapping("/{productId}/submit-qc")
    public ResponseEntity<ApiResponse<ProductDtos.SubmitResponse>> submit(
            @PathVariable String productId,
            @RequestBody ProductDtos.SubmitRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        ProductDtos.SubmitResponse response = idempotency.execute(idempotencyKey,
                "POST /vendor/products/{id}/submit-qc", request, ProductDtos.SubmitResponse.class,
                () -> submitService.submit(productId, request));
        return ResponseEntity.accepted().body(ApiResponse.of(response));
    }

    @Operation(summary = "Withdraw from the QC queue and re-open the save endpoints")
    @PostMapping("/{productId}/withdraw")
    public ApiResponse<ProductDtos.WithdrawResponse> withdraw(
            @PathVariable String productId,
            @RequestBody ProductDtos.WithdrawRequest request) {
        return ApiResponse.of(submitService.withdraw(productId, request));
    }
}
