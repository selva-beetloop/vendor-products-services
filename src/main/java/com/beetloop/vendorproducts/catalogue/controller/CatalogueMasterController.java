package com.beetloop.vendorproducts.catalogue.controller;

import com.beetloop.vendorproducts.catalogue.CatalogueService;
import com.beetloop.vendorproducts.catalogue.domain.CatalogueKind;
import com.beetloop.vendorproducts.catalogue.dto.CatalogueDtos;
import com.beetloop.vendorproducts.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vendor")
@Tag(name = "Catalogue Masters", description = "T1 Scientific and T2 Commercial masters.")
public class CatalogueMasterController {

    private final CatalogueService catalogue;

    public CatalogueMasterController(CatalogueService catalogue) {
        this.catalogue = catalogue;
    }

    @GetMapping("/catalog/commercial-masters")
    @PreAuthorize("hasAnyRole('VENDOR','QC_ADMIN','QC_USER','INTEL_QC','INTEL_ADMIN')")
    @Operation(summary = "Search approved / live T2 commercial masters")
    public PageResponse<CatalogueDtos.CommercialMasterResponse> commercialMasters(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) CatalogueKind kind,
            @RequestParam(defaultValue = "true") boolean liveOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return catalogue.searchCommercial(search, category, kind, page, size, liveOnly);
    }

    @GetMapping("/catalog/scientific-masters")
    @PreAuthorize("hasAnyRole('VENDOR','QC_ADMIN','QC_USER','INTEL_QC','INTEL_ADMIN')")
    @Operation(summary = "Search approved / live T1 scientific masters")
    public PageResponse<CatalogueDtos.ScientificMasterResponse> scientificMasters(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) CatalogueKind kind,
            @RequestParam(defaultValue = "true") boolean liveOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return catalogue.searchScientific(search, category, kind, page, size, liveOnly);
    }

    @PostMapping("/catalog/scientific-masters")
    @PreAuthorize("hasAnyRole('VENDOR','INTEL_QC','INTEL_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogueDtos.ScientificMasterResponse createScientific(
            @RequestBody CatalogueDtos.CreateScientificRequest request) {
        return catalogue.createScientific(request);
    }

    @PostMapping("/catalog/commercial-masters")
    @PreAuthorize("hasAnyRole('VENDOR','INTEL_QC','INTEL_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogueDtos.CommercialMasterResponse createCommercial(
            @RequestBody CatalogueDtos.CreateCommercialRequest request) {
        return catalogue.createCommercial(request, true);
    }

    @PostMapping("/commercial-masters/{code}/branch")
    @PreAuthorize("hasAnyRole('VENDOR','INTEL_QC','INTEL_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Grade-defining branch — never mutates the shared T2")
    public CatalogueDtos.CommercialMasterResponse branch(
            @PathVariable String code,
            @Valid @RequestBody CatalogueDtos.BranchRequest request) {
        return catalogue.branch(code, request);
    }
}
