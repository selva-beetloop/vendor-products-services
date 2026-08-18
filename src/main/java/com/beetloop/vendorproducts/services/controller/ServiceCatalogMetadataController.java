package com.beetloop.vendorproducts.services.controller;

import com.beetloop.vendorproducts.services.domain.ServiceCategory;
import com.beetloop.vendorproducts.services.registry.ServiceFieldRegistry;
import com.beetloop.vendorproducts.services.registry.ServiceSchema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Publishes the Services field inventory so the frontend can render or
 * cross-check a wizard, and so the per-category schema is part of the API
 * contract rather than buried in the codebase.
 */
@RestController
@RequestMapping("/api/vendor/catalog")
@Tag(name = "Service Catalog Metadata", description = "Service categories, stages and field definitions.")
public class ServiceCatalogMetadataController {

    private final ServiceFieldRegistry registry;

    public ServiceCatalogMetadataController(ServiceFieldRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/service-categories")
    @PreAuthorize("hasAnyRole('VENDOR','QC_ADMIN','QC_USER','INTEL_QC','INTEL_ADMIN')")
    @Operation(summary = "All five service categories with their stages and fields")
    public Map<String, Object> categories() {
        return registry.categoryCatalog();
    }

    @GetMapping("/service-categories/{category}")
    @PreAuthorize("hasAnyRole('VENDOR','QC_ADMIN','QC_USER','INTEL_QC','INTEL_ADMIN')")
    @Operation(summary = "One service category's full schema")
    public ServiceSchema.Category category(@PathVariable ServiceCategory category) {
        return registry.category(category);
    }

    @GetMapping("/service-categories/{category}/stages")
    @PreAuthorize("hasAnyRole('VENDOR','QC_ADMIN','QC_USER','INTEL_QC','INTEL_ADMIN')")
    @Operation(summary = "Stage keys for a category, in wizard order",
            description = "Counts read from the UI: four categories have 4 stages, CRO has 11.")
    public List<String> stages(@PathVariable ServiceCategory category) {
        return registry.stageKeys(category);
    }

    @GetMapping("/service-categories/{category}/stages/{stageKey}/fields")
    @PreAuthorize("hasAnyRole('VENDOR','QC_ADMIN','QC_USER','INTEL_QC','INTEL_ADMIN')")
    @Operation(summary = "Flattened field list for one stage")
    public List<ServiceSchema.Field> stageFields(@PathVariable ServiceCategory category,
                                                 @PathVariable String stageKey) {
        return registry.stageFields(category, stageKey);
    }

    @GetMapping("/service-categories/{category}/document-kinds")
    @PreAuthorize("hasAnyRole('VENDOR','QC_ADMIN','QC_USER','INTEL_QC','INTEL_ADMIN')")
    @Operation(summary = "Which document modals this category offers, with their fields",
            description = "Empty lists are meaningful: Contract Manufacturer has no accreditation kind, "
                    + "and Consultancy offers none of the three.")
    public Map<String, Object> documentKinds(@PathVariable ServiceCategory category) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (com.beetloop.vendorproducts.services.domain.ServiceDocument.Kind kind
                : com.beetloop.vendorproducts.services.domain.ServiceDocument.Kind.values()) {
            out.put(kind.name(), registry.documentFields(category, kind));
        }
        return out;
    }
}
