package com.beetloop.catalog.servicelisting.web;

import com.beetloop.catalog.document.DocumentDtos;
import com.beetloop.catalog.document.DocumentLinkService;
import com.beetloop.catalog.servicelisting.ServiceConfigurationService;
import com.beetloop.catalog.servicelisting.dto.ServiceDtos;
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
import java.util.List;

@Tag(name = "Service configurations",
        description = "The N selected services inside a listing, each with its own sub-steps")
@RestController
@RequestMapping("/vendor/services/{serviceListingId}")
public class ServiceConfigurationController {

    private final ServiceConfigurationService configurationService;
    private final DocumentLinkService linkService;

    public ServiceConfigurationController(ServiceConfigurationService configurationService,
                                          DocumentLinkService linkService) {
        this.configurationService = configurationService;
        this.linkService = linkService;
    }

    @Operation(summary = "The Selected Services (N) table on outer step 2")
    @GetMapping("/configurations")
    public PagedResponse<ServiceDtos.ConfigurationRow> list(
            @PathVariable String serviceListingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return configurationService.list(serviceListingId, page, size);
    }

    @Operation(summary = "Add a service to the listing (Path A master row, or Path B request)")
    @PostMapping("/configurations")
    public ResponseEntity<ApiResponse<ServiceDtos.ConfigurationResponse>> add(
            @PathVariable String serviceListingId,
            @RequestBody ServiceDtos.AddConfigurationRequest request) {
        ServiceDtos.ConfigurationResponse response = configurationService.add(serviceListingId, request);
        return ResponseEntity.created(URI.create("/api/v1/vendor/services/%s/configurations/%s"
                        .formatted(serviceListingId, response.configurationId())))
                .body(ApiResponse.of(response));
    }

    @GetMapping("/configurations/{configurationId}")
    public ApiResponse<ServiceDtos.ConfigurationResponse> get(@PathVariable String serviceListingId,
                                                              @PathVariable String configurationId) {
        return ApiResponse.of(configurationService.get(serviceListingId, configurationId));
    }

    @Operation(summary = "SECTION SAVE: one configuration sub-step, siblings untouched")
    @PutMapping("/configurations/{configurationId}/sections/{sectionKey}")
    public ApiResponse<ServiceDtos.SectionSaveResponse> saveSection(
            @PathVariable String serviceListingId,
            @PathVariable String configurationId,
            @PathVariable String sectionKey,
            @RequestBody ServiceDtos.SectionSaveRequest request) {
        return ApiResponse.of(configurationService.saveSection(serviceListingId, configurationId,
                sectionKey, request));
    }

    @Operation(summary = "Whole-configuration save: every sub-step in one call")
    @PutMapping("/configurations/{configurationId}")
    public ApiResponse<ServiceDtos.ConfigurationWholeSaveResponse> saveWhole(
            @PathVariable String serviceListingId,
            @PathVariable String configurationId,
            @RequestBody ServiceDtos.ConfigurationWholeSaveRequest request) {
        return ApiResponse.of(configurationService.saveWhole(serviceListingId, configurationId, request));
    }

    @DeleteMapping("/configurations/{configurationId}")
    public ResponseEntity<Void> delete(@PathVariable String serviceListingId,
                                       @PathVariable String configurationId) {
        configurationService.delete(serviceListingId, configurationId);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ document links

    @GetMapping("/document-links")
    public ApiResponse<List<DocumentDtos.DocumentLinkResponse>> links(
            @PathVariable String serviceListingId) {
        return ApiResponse.of(linkService.list(serviceListingId));
    }

    @Operation(summary = "Link a library document. selectionId null means lab-wide.")
    @PostMapping("/document-links")
    public ResponseEntity<ApiResponse<DocumentDtos.DocumentLinkResponse>> link(
            @PathVariable String serviceListingId,
            @Valid @RequestBody DocumentDtos.DocumentLinkRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.of(linkService.link(serviceListingId, request)));
    }

    @DeleteMapping("/document-links/{linkId}")
    public ResponseEntity<Void> unlink(@PathVariable String serviceListingId,
                                       @PathVariable String linkId) {
        linkService.unlink(serviceListingId, linkId);
        return ResponseEntity.noContent().build();
    }
}
