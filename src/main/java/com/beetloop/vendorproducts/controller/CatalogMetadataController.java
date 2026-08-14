package com.beetloop.vendorproducts.controller;

import com.beetloop.vendorproducts.domain.ProductCategory;
import com.beetloop.vendorproducts.registry.CategoryFieldRegistry;
import com.beetloop.vendorproducts.registry.CategorySchema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Publishes the field inventory itself. Useful for the frontend (to render or
 * cross-check a form), and it makes the category-specific schema part of the API
 * contract rather than something buried in the codebase.
 */
@RestController
@RequestMapping("/api/vendor/catalog")
@Tag(name = "Catalog Metadata", description = "Category, type-card, role-card and field definitions.")
public class CatalogMetadataController {

    private final CategoryFieldRegistry registry;

    public CatalogMetadataController(CategoryFieldRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/categories")
    @Operation(summary = "All five categories with their steps, type cards, role cards and fields")
    public Map<String, Object> categories() {
        return registry.categoryCatalog();
    }

    @GetMapping("/categories/{category}")
    @Operation(summary = "One category's full schema")
    public CategorySchema.Category category(@PathVariable ProductCategory category) {
        return registry.category(category);
    }

    @GetMapping("/categories/{category}/identity-types")
    @Operation(summary = "Type cards available in Step 1 for this category",
            description = "Empty for categories whose Step 1 has no type selector.")
    public List<String> identityTypes(@PathVariable ProductCategory category) {
        return registry.identityTypeIds(category);
    }

    @GetMapping("/categories/{category}/roles")
    @Operation(summary = "Role cards available in Step 2 for this category")
    public List<String> roles(@PathVariable ProductCategory category) {
        return registry.roleIds(category);
    }

    @GetMapping("/categories/{category}/fields")
    @Operation(summary = "Flattened field inventory for one category",
            description = "Identity base fields, per-type-card fields, shared role fields and variant "
                    + "fields — the category-wise field mapping in machine-readable form.")
    public Map<String, Object> fields(@PathVariable ProductCategory category) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("identityBaseFields", registry.identityBaseFields(category));

        Map<String, Object> types = new LinkedHashMap<>();
        for (String typeId : registry.identityTypeIds(category)) {
            types.put(typeId, registry.identityTypeFields(category, typeId));
        }
        out.put("identityTypes", types);

        Map<String, Object> roles = new LinkedHashMap<>();
        for (String roleId : registry.roleIds(category)) {
            roles.put(roleId, registry.roleFields(category, roleId));
        }
        out.put("roles", roles);

        out.put("variantDetailFields", registry.variantDetailFields(category));
        out.put("variantExtraFields", registry.variantExtraFields(category));
        return out;
    }

    @GetMapping("/shared-variant-sections")
    @Operation(summary = "Field definitions shared by every category's variant sub-steps",
            description = "Technical specifications, commercial & pricing, compliance and marketplace.")
    public Map<String, Object> sharedVariantSections() {
        return registry.raw().sharedVariantSections();
    }
}
