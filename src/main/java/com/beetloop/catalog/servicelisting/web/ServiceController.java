package com.beetloop.catalog.servicelisting.web;

import com.beetloop.catalog.servicelisting.ServiceDraftService;
import com.beetloop.catalog.servicelisting.ServiceGuard;
import com.beetloop.catalog.servicelisting.ServiceMapper;
import com.beetloop.catalog.servicelisting.ServiceOverallSaveService;
import com.beetloop.catalog.servicelisting.ServiceReviewService;
import com.beetloop.catalog.servicelisting.ServiceStepSaveService;
import com.beetloop.catalog.servicelisting.ServiceSubmitService;
import com.beetloop.catalog.servicelisting.dto.ServiceDtos;
import com.beetloop.catalog.servicelisting.model.ServiceListing;
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

@Tag(name = "Services", description = "Vendor service listings: draft, step-wise save, overall save, submit")
@RestController
@RequestMapping("/vendor/services")
public class ServiceController {

    private final ServiceDraftService draftService;
    private final ServiceStepSaveService stepSaveService;
    private final ServiceOverallSaveService overallSaveService;
    private final ServiceReviewService reviewService;
    private final ServiceSubmitService submitService;
    private final IdempotencyService idempotency;

    public ServiceController(ServiceDraftService draftService, ServiceStepSaveService stepSaveService,
                             ServiceOverallSaveService overallSaveService,
                             ServiceReviewService reviewService, ServiceSubmitService submitService,
                             IdempotencyService idempotency) {
        this.draftService = draftService;
        this.stepSaveService = stepSaveService;
        this.overallSaveService = overallSaveService;
        this.reviewService = reviewService;
        this.submitService = submitService;
        this.idempotency = idempotency;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ServiceDtos.ServiceResponse>> create(
            @Valid @RequestBody ServiceDtos.CreateServiceRequest request) {
        ServiceListing listing = draftService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/vendor/services/" + listing.getId()))
                .eTag(ServiceGuard.etag(listing))
                .body(ApiResponse.of(ServiceMapper.toResponse(listing, draftService.template(listing))));
    }

    @GetMapping("/{serviceListingId}")
    public ResponseEntity<ApiResponse<ServiceDtos.ServiceResponse>> get(
            @PathVariable String serviceListingId) {
        ServiceListing listing = draftService.get(serviceListingId);
        return ResponseEntity.ok().eTag(ServiceGuard.etag(listing))
                .body(ApiResponse.of(ServiceMapper.toResponse(listing, draftService.template(listing))));
    }

    @DeleteMapping("/{serviceListingId}")
    public ResponseEntity<Void> delete(@PathVariable String serviceListingId) {
        draftService.delete(serviceListingId);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ STEP-WISE SAVE

    @GetMapping("/{serviceListingId}/steps/{stepKey}")
    public ApiResponse<ServiceDtos.StepReadResponse> readStep(@PathVariable String serviceListingId,
                                                              @PathVariable String stepKey) {
        return ApiResponse.of(stepSaveService.read(serviceListingId, stepKey));
    }

    @Operation(summary = "STEP-WISE SAVE: select-service | configure-services | compliance | review")
    @PutMapping("/{serviceListingId}/steps/{stepKey}")
    public ResponseEntity<ApiResponse<ServiceDtos.StepSaveResponse>> saveStep(
            @PathVariable String serviceListingId,
            @PathVariable String stepKey,
            @RequestBody ServiceDtos.StepSaveRequest request,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        ServiceDtos.StepSaveResponse response =
                stepSaveService.save(serviceListingId, stepKey, request, ifMatch);
        return ResponseEntity.ok().eTag(response.etag()).body(ApiResponse.of(response));
    }

    @PostMapping("/{serviceListingId}/steps/{stepKey}/validate")
    public ApiResponse<ServiceDtos.ValidateStepResponse> validateStep(
            @PathVariable String serviceListingId,
            @PathVariable String stepKey,
            @RequestBody ServiceDtos.StepSaveRequest request) {
        return ApiResponse.of(stepSaveService.validate(serviceListingId, stepKey, request));
    }

    // ------------------------------------------------------------------ OVERALL SAVE

    @Operation(summary = "OVERALL SAVE (create or update): all four steps and every configuration, "
            + "one transaction. Full replace. Still a draft write - this is NOT submit.")
    @PostMapping("/save-all")
    public ResponseEntity<ApiResponse<ServiceDtos.SaveAllResponse>> saveAllCreate(
            @Valid @RequestBody ServiceDtos.SaveAllRequest request,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        boolean creating = request.serviceListingId() == null;
        ServiceDtos.SaveAllResponse response = idempotency.execute(idempotencyKey,
                "POST /vendor/services/save-all", request, ServiceDtos.SaveAllResponse.class,
                () -> overallSaveService.saveAll(null, request, ifMatch, false));
        ResponseEntity.BodyBuilder builder = creating
                ? ResponseEntity.status(HttpStatus.CREATED)
                        .location(URI.create("/api/v1/vendor/services/" + response.serviceListingId()))
                : ResponseEntity.ok();
        return builder.eTag(response.etag()).body(ApiResponse.of(response));
    }

    @Operation(summary = "OVERALL SAVE (update). If-Match is REQUIRED.")
    @PutMapping("/{serviceListingId}/save-all")
    public ResponseEntity<ApiResponse<ServiceDtos.SaveAllResponse>> saveAllUpdate(
            @PathVariable String serviceListingId,
            @Valid @RequestBody ServiceDtos.SaveAllRequest request,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        ServiceDtos.SaveAllResponse response = idempotency.execute(idempotencyKey,
                "PUT /vendor/services/{id}/save-all", request, ServiceDtos.SaveAllResponse.class,
                () -> overallSaveService.saveAll(serviceListingId, request, ifMatch, true));
        return ResponseEntity.ok().eTag(response.etag()).body(ApiResponse.of(response));
    }

    // ------------------------------------------------------------------ review & submit

    @GetMapping("/{serviceListingId}/review")
    public ApiResponse<ServiceDtos.ReviewResponse> review(@PathVariable String serviceListingId) {
        return ApiResponse.of(reviewService.review(serviceListingId));
    }

    @Operation(summary = "Submit for QC / Submit for review / Review & Submit - one endpoint")
    @PostMapping("/{serviceListingId}/submit-qc")
    public ResponseEntity<ApiResponse<ServiceDtos.SubmitResponse>> submit(
            @PathVariable String serviceListingId,
            @RequestBody ServiceDtos.SubmitRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        ServiceDtos.SubmitResponse response = idempotency.execute(idempotencyKey,
                "POST /vendor/services/{id}/submit-qc", request, ServiceDtos.SubmitResponse.class,
                () -> submitService.submit(serviceListingId, request));
        return ResponseEntity.accepted().body(ApiResponse.of(response));
    }

    @PostMapping("/{serviceListingId}/withdraw")
    public ApiResponse<ServiceDtos.WithdrawResponse> withdraw(
            @PathVariable String serviceListingId,
            @RequestBody ServiceDtos.WithdrawRequest request) {
        return ApiResponse.of(submitService.withdraw(serviceListingId, request));
    }
}
