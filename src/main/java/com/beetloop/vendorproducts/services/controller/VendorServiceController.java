package com.beetloop.vendorproducts.services.controller;

import com.beetloop.vendorproducts.dto.ApiError;
import com.beetloop.vendorproducts.dto.PageResponse;
import com.beetloop.vendorproducts.services.domain.ServiceCategory;
import com.beetloop.vendorproducts.services.domain.ServiceStatus;
import com.beetloop.vendorproducts.services.dto.ServiceDtos;
import com.beetloop.vendorproducts.services.service.VendorServiceCatalogService;
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
import java.util.UUID;

/**
 * Vendor Services endpoints, mounted at
 * {@code http://localhost:8086/vendor-products/api/vendor/services}.
 *
 * <p>Mirrors the shape already proven by the products module: create a draft,
 * save each wizard stage independently, or save the whole batch in one
 * transactional call, then submit for QC.
 */
@RestController
@RequestMapping("/api/vendor/services")
@Tag(name = "Vendor Services", description = "Add Service wizard: stage saves, overall save, documents and QC.")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation failed",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Batch, service or document not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Action not allowed from the current status",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
})
public class VendorServiceController {

    private final VendorServiceCatalogService service;

    public VendorServiceController(VendorServiceCatalogService service) {
        this.service = service;
    }

    // ---------------------------------------------------------------- create

    @PostMapping
    @Operation(summary = "Create a draft service batch",
            description = "Called when the vendor picks a category on the 'List a Service' chooser. "
                    + "A batch can hold several services — every category supports adding more than one.")
    public ResponseEntity<ServiceDtos.BatchResponse> create(
            @Valid @RequestBody ServiceDtos.CreateBatchRequest request,
            @RequestHeader(value = "X-VENDOR-ID", required = false) String vendorId,
            @RequestHeader(value = "X-USER-ID", required = false) String userId) {
        ServiceDtos.BatchResponse created = service.createBatch(request, vendorId, userId);
        return ResponseEntity.created(URI.create("/api/vendor/services/" + created.id())).body(created);
    }

    // ----------------------------------------------------------------- reads

    @GetMapping
    @Operation(summary = "List services",
            description = "Backs the Services catalog table. Free-text search matches name, SKU and "
                    + "category — the same three fields the listing page itself searches.")
    public PageResponse<ServiceDtos.ServiceSummaryResponse> list(
            @RequestHeader(value = "X-VENDOR-ID", required = false) String vendorId,
            @Parameter(description = "lab-testing | consultancy | contract-manufacturer | "
                    + "agro-processing | cro")
            @RequestParam(required = false) ServiceCategory category,
            @Parameter(description = "DRAFT | SUBMITTED_FOR_QC | PUBLISHED | … or a StatusKind such as qc-pending")
            @RequestParam(required = false) ServiceStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {
        return service.listServices(vendorId, category, status, search, page, size, sort);
    }

    @GetMapping("/qc-review")
    @Operation(summary = "QC review queue", description = "Service batches awaiting review.")
    public PageResponse<ServiceDtos.ServiceSummaryResponse> qcQueue(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {
        return service.qcQueue(search, page, size, sort);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a service batch with every stage and service",
            description = "Enough to rehydrate the whole wizard.")
    public ServiceDtos.BatchResponse get(@PathVariable UUID id) {
        return service.getBatch(id);
    }

    // ----------------------------------------------------- step-based saves

    @PutMapping("/{id}/stages/{stageKey}")
    @Operation(summary = "Save one wizard stage",
            description = "Validates only this stage. `data` is the batch-level payload; `items` carries "
                    + "per-service payloads, which is how Configure Services saves several services at "
                    + "once. Send `draft: true` to store partial progress.")
    public ServiceDtos.BatchResponse saveStage(@PathVariable UUID id,
                                               @PathVariable String stageKey,
                                               @Valid @RequestBody ServiceDtos.StageSaveRequest request) {
        return service.saveStage(id, stageKey, request);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(summary = "Remove a service from the batch")
    public ResponseEntity<Void> deleteItem(@PathVariable UUID id, @PathVariable UUID itemId) {
        service.deleteItem(id, itemId);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------ overall save

    @PostMapping("/{id}/save")
    @Operation(summary = "Overall save",
            description = "Writes every stage and every service in a single transaction. Stages omitted "
                    + "keep whatever the stage saves stored. Set `submitForQc: true` to submit in the "
                    + "same call.")
    public ServiceDtos.BatchResponse saveAll(@PathVariable UUID id,
                                             @Valid @RequestBody ServiceDtos.OverallSaveRequest request) {
        return service.saveAll(id, request);
    }

    // -------------------------------------------------------------- documents

    @PostMapping("/{id}/items/{itemId}/documents")
    @Operation(summary = "Add an accreditation, certification or support document",
            description = "`kind` selects the field set. A kind the category does not offer is rejected — "
                    + "Contract Manufacturer has no accreditation modal and Consultancy has none of the three.")
    public ResponseEntity<ServiceDtos.ServiceResponse> addDocument(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @Valid @RequestBody ServiceDtos.DocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addDocument(id, itemId, request));
    }

    @PutMapping("/{id}/items/{itemId}/documents/{documentId}")
    @Operation(summary = "Update a document (Manage Documents → edit)")
    public ServiceDtos.ServiceResponse updateDocument(@PathVariable UUID id,
                                                      @PathVariable UUID itemId,
                                                      @PathVariable UUID documentId,
                                                      @Valid @RequestBody ServiceDtos.DocumentRequest request) {
        return service.updateDocument(id, itemId, documentId, request);
    }

    @DeleteMapping("/{id}/items/{itemId}/documents/{documentId}")
    @Operation(summary = "Delete a document (Manage Documents → delete)")
    public ServiceDtos.ServiceResponse deleteDocument(@PathVariable UUID id,
                                                      @PathVariable UUID itemId,
                                                      @PathVariable UUID documentId) {
        return service.deleteDocument(id, itemId, documentId);
    }

    // ------------------------------------------------------------ QC workflow

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit the batch",
            description = "Routes to QC review when the batch contains a custom service, mirroring the "
                    + "wizard's own conditional button; otherwise publishes straight to the catalog.")
    public ServiceDtos.BatchResponse submit(@PathVariable UUID id) {
        return service.submit(id);
    }

    @PutMapping("/{id}/qc-decision")
    @Operation(summary = "Record a QC decision",
            description = "APPROVE / REJECT / QUERY / PUBLISH. REJECT and QUERY require remarks.")
    public ServiceDtos.BatchResponse qcDecision(@PathVariable UUID id,
                                                @Valid @RequestBody ServiceDtos.QcDecisionRequest request) {
        return service.qcDecision(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a service batch")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteBatch(id);
        return ResponseEntity.noContent().build();
    }
}
