package com.beetloop.vendorproducts.catalogue.controller;

import com.beetloop.vendorproducts.catalogue.CatalogueService;
import com.beetloop.vendorproducts.catalogue.dto.CatalogueDtos;
import com.beetloop.vendorproducts.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/vendor/intelligence")
@Tag(name = "Intelligence QC", description = "T1/T2 Intelligence QC queue — distinct from Vendor QC.")
public class IntelligenceQcController {

    private final CatalogueService catalogue;

    public IntelligenceQcController(CatalogueService catalogue) {
        this.catalogue = catalogue;
    }

    @GetMapping("/qc-review")
    @PreAuthorize("hasAnyRole('INTEL_QC','INTEL_ADMIN')")
    @Operation(summary = "Intelligence QC queue for T1 and T2")
    public PageResponse<CatalogueDtos.IntelQcRow> queue(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return catalogue.intelQueue(search, page, size);
    }

    @PutMapping("/qc-decision")
    @PreAuthorize("hasAnyRole('INTEL_QC','INTEL_ADMIN')")
    public void decision(@Valid @RequestBody CatalogueDtos.IntelQcDecisionRequest request) {
        catalogue.intelDecision(request.kind(), request.id(), request);
    }

    @PutMapping("/scientific-masters/{id}/qc-decision")
    @PreAuthorize("hasAnyRole('INTEL_QC','INTEL_ADMIN')")
    public void scientificDecision(@PathVariable UUID id,
                                   @RequestBody CatalogueDtos.IntelQcDecisionRequest request) {
        catalogue.intelDecision("T1", id, request);
    }

    @PutMapping("/commercial-masters/{id}/qc-decision")
    @PreAuthorize("hasAnyRole('INTEL_QC','INTEL_ADMIN')")
    public void commercialDecision(@PathVariable UUID id,
                                   @RequestBody CatalogueDtos.IntelQcDecisionRequest request) {
        catalogue.intelDecision("T2", id, request);
    }
}
