package com.beetloop.catalog.product;

import com.beetloop.catalog.audit.AuditEvent;
import com.beetloop.catalog.audit.AuditService;
import com.beetloop.catalog.config.CatalogProperties;
import com.beetloop.catalog.product.dto.ProductDtos;
import com.beetloop.catalog.product.model.ProductListing;
import com.beetloop.catalog.product.model.ProductVariant;
import com.beetloop.catalog.shared.api.PageMeta;
import com.beetloop.catalog.shared.api.PagedResponse;
import com.beetloop.catalog.shared.error.ApiException;
import com.beetloop.catalog.shared.error.ErrorCode;
import com.beetloop.catalog.shared.util.Ids;
import com.beetloop.catalog.shared.util.Maps;
import com.beetloop.catalog.template.TemplateService;
import com.beetloop.catalog.template.ValidationEngine;
import com.beetloop.catalog.template.ValidationMode;
import com.beetloop.catalog.template.ValidationResult;
import com.beetloop.catalog.template.model.FormTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Variant CRUD plus the stage-scoped SECTION SAVE - the variant-level analogue of a step save. */
@Service
public class ProductVariantService {

    private final ProductListingRepository repository;
    private final ProductGuard guard;
    private final TemplateService templates;
    private final ValidationEngine validationEngine;
    private final ProductRecalculator recalculator;
    private final CatalogProperties properties;
    private final AuditService audit;

    public ProductVariantService(ProductListingRepository repository, ProductGuard guard,
                                 TemplateService templates, ValidationEngine validationEngine,
                                 ProductRecalculator recalculator, CatalogProperties properties,
                                 AuditService audit) {
        this.repository = repository;
        this.guard = guard;
        this.templates = templates;
        this.validationEngine = validationEngine;
        this.recalculator = recalculator;
        this.properties = properties;
        this.audit = audit;
    }

    // ------------------------------------------------------------------ read

    public PagedResponse<ProductDtos.VariantResponse> list(String productId, int page, int size,
                                                           String status, String variantType,
                                                           String groupBy) {
        ProductListing listing = guard.load(productId);
        FormTemplate template = template(listing);

        List<ProductVariant> filtered = listing.getVariants().stream()
                .filter(v -> status == null || "ALL".equalsIgnoreCase(status)
                        || status.equalsIgnoreCase(v.getStatus()))
                .filter(v -> variantType == null || "ALL".equalsIgnoreCase(variantType)
                        || variantType.equalsIgnoreCase(detailValue(v, "variantType")))
                .toList();

        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        List<ProductDtos.VariantResponse> pageContent =
                ProductMapper.toVariantResponses(filtered.subList(from, to));

        Map<String, Object> step = Maps.orEmpty(listing.step("variants"));
        ProductDtos.VariantListMeta meta = new ProductDtos.VariantListMeta(
                Maps.asMap(step.get("counters")),
                Maps.asMap(step.get("groupings")),
                gridColumns(template),
                groups(filtered, groupBy));

        return PagedResponse.of(pageContent, PageMeta.of(page, size, filtered.size()), meta);
    }

    public ProductDtos.VariantResponse get(String productId, String variantId) {
        return ProductMapper.toVariantResponse(requireVariant(guard.load(productId), variantId));
    }

    // ------------------------------------------------------------------ write

    /** The "Add Variant" button opens an empty builder, so an empty body is valid. */
    public ProductDtos.VariantResponse create(String productId, ProductDtos.CreateVariantRequest request) {
        ProductListing listing = guard.loadEditable(productId);
        FormTemplate template = template(listing);
        checkCap(listing);

        Instant now = Instant.now();
        ProductVariant variant = ProductVariant.builder()
                .variantId(Ids.newId("var"))
                .sections(seedSections(template, request == null ? null : request.sections()))
                .status("DRAFT")
                .createdAt(now)
                .updatedAt(now)
                .build();

        listing.getVariants().add(variant);
        recalculator.recompute(listing, template);
        repository.save(listing);
        return ProductMapper.toVariantResponse(variant);
    }

    /**
     * SECTION SAVE. Fires on "Next" in the variant builder: replaces one stage, siblings untouched.
     */
    public ProductDtos.VariantSectionSaveResponse saveSection(String productId, String variantId,
                                                              String sectionKey,
                                                              ProductDtos.VariantSectionSaveRequest request) {
        ProductListing listing = guard.loadEditable(productId);
        FormTemplate template = template(listing);
        FormTemplate.SectionSchema section = templates.requireChildSection(template, sectionKey);
        ProductVariant variant = requireVariant(listing, variantId);

        String dataKey = ProductRecalculator.dataKeyOf(section);
        Map<String, Object> existing = Maps.orEmpty(variant.section(dataKey));
        ValidationResult result = validationEngine.validateSection(section, Maps.orEmpty(request.data()),
                existing, ValidationMode.SAVE, "variants",
                "variants[%s].sections.%s".formatted(variantId, dataKey));
        result.throwIfInvalid();

        variant.getSections().put(dataKey, result.sanitized());
        variant.setUpdatedAt(Instant.now());
        recalculator.recompute(listing, template);
        ProductListing saved = repository.save(listing);
        ProductVariant reloaded = requireVariant(saved, variantId);

        audit.record(AuditEvent.VARIANT_SECTION_SAVED, "PRODUCT_LISTING", saved.getId(),
                Map.of("variantId", variantId, "sectionKey", section.key()));

        return new ProductDtos.VariantSectionSaveResponse(
                variantId, section.key(), saved.versionOrZero(), reloaded.getCompletionPercent(),
                reloaded.getStatus(), Maps.orEmpty(reloaded.section(dataKey)),
                result.rejectedFields(), result.warnings());
    }

    /** Whole-variant save - the variant-scope analogue of save-all. Fires on the final "Add Variant". */
    public ProductDtos.VariantWholeSaveResponse saveWhole(String productId, String variantId,
                                                          ProductDtos.VariantWholeSaveRequest request) {
        ProductListing listing = guard.loadEditable(productId);
        FormTemplate template = template(listing);
        ProductVariant variant = requireVariant(listing, variantId);

        List<com.beetloop.catalog.shared.error.RejectedField> rejected = new ArrayList<>();
        List<com.beetloop.catalog.shared.error.Warning> warnings = new ArrayList<>();
        List<com.beetloop.catalog.shared.error.FieldError> errors = new ArrayList<>();
        List<String> savedSections = new ArrayList<>();
        Map<String, Object> incoming = Maps.orEmpty(request.sections());
        Map<String, Object> rebuilt = new LinkedHashMap<>();

        for (FormTemplate.SectionSchema section : template.childSections()) {
            String dataKey = ProductRecalculator.dataKeyOf(section);
            if (!incoming.containsKey(dataKey)) {
                continue;
            }
            ValidationResult result = validationEngine.validateSection(section,
                    Maps.orEmpty(Maps.asMap(incoming.get(dataKey))),
                    Maps.orEmpty(variant.section(dataKey)), ValidationMode.SAVE, "variants",
                    "variants[%s].sections.%s".formatted(variantId, dataKey));
            errors.addAll(result.errors());
            rejected.addAll(result.rejectedFields());
            warnings.addAll(result.warnings());
            rebuilt.put(dataKey, result.sanitized());
            savedSections.add(dataKey);
        }
        if (!errors.isEmpty()) {
            throw new com.beetloop.catalog.shared.error.ValidationException(errors, warnings);
        }

        rebuilt.forEach(variant.getSections()::put);
        variant.setUpdatedAt(Instant.now());
        recalculator.recompute(listing, template);
        ProductListing saved = repository.save(listing);
        ProductVariant reloaded = requireVariant(saved, variantId);

        return new ProductDtos.VariantWholeSaveResponse(variantId, saved.versionOrZero(),
                reloaded.getCompletionPercent(), reloaded.getStatus(), savedSections, rejected, warnings);
    }

    public void delete(String productId, String variantId) {
        ProductListing listing = guard.loadEditable(productId);
        FormTemplate template = template(listing);
        boolean removed = listing.getVariants().removeIf(v -> v.getVariantId().equals(variantId));
        if (!removed) {
            throw ApiException.notFound("Variant " + variantId);
        }
        recalculator.recompute(listing, template);
        repository.save(listing);
    }

    /** Publish / Archive / Delete / Mark as Draft / Export - reported per id, never half-applied silently. */
    public ProductDtos.BulkActionResponse bulkAction(String productId,
                                                     ProductDtos.BulkActionRequest request) {
        ProductListing listing = guard.loadEditable(productId);
        FormTemplate template = template(listing);
        List<ProductDtos.BulkResult> results = new ArrayList<>();
        int succeeded = 0;

        for (String variantId : request.variantIds() == null ? List.<String>of() : request.variantIds()) {
            ProductVariant variant = listing.getVariants().stream()
                    .filter(v -> v.getVariantId().equals(variantId)).findFirst().orElse(null);
            if (variant == null) {
                results.add(new ProductDtos.BulkResult(variantId, "FAILED",
                        ErrorCode.NOT_FOUND.code(), "Variant not found."));
                continue;
            }
            switch (request.action().toUpperCase()) {
                case "PUBLISH" -> {
                    if (variant.getCompletionPercent() < 100) {
                        results.add(new ProductDtos.BulkResult(variantId, "FAILED",
                                ErrorCode.VALIDATION.code(),
                                "Variant is %d%% complete.".formatted(variant.getCompletionPercent())));
                        continue;
                    }
                    variant.setStatus("ACTIVE");
                }
                case "ARCHIVE" -> variant.setStatus("INACTIVE");
                case "MARK_AS_DRAFT" -> variant.setStatus("DRAFT");
                case "DELETE" -> listing.getVariants().remove(variant);
                default -> throw new ApiException(ErrorCode.VALIDATION,
                        "Unsupported bulk action '%s'.".formatted(request.action()));
            }
            succeeded++;
            results.add(new ProductDtos.BulkResult(variantId, "OK", null, null));
        }

        recalculator.recompute(listing, template);
        repository.save(listing);
        int requested = results.size();
        return new ProductDtos.BulkActionResponse(request.action().toUpperCase(), requested, succeeded,
                requested - succeeded, results);
    }

    // ------------------------------------------------------------------ internals

    private void checkCap(ProductListing listing) {
        int cap = properties.getLimits().getMaxVariantsPerListing();
        if (listing.getVariants().size() >= cap) {
            throw new ApiException(ErrorCode.COLLECTION_CAP,
                    "A listing may hold at most %d variants.".formatted(cap));
        }
    }

    /**
     * Seeded specification groups come from the form template, not from empty scaffolding - the
     * running UI ships them pre-populated with method and unit, so they are master data.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> seedSections(FormTemplate template, Map<String, Object> provided) {
        Map<String, Object> sections = new LinkedHashMap<>();
        for (FormTemplate.SectionSchema section : template.childSections()) {
            sections.put(ProductRecalculator.dataKeyOf(section), new LinkedHashMap<String, Object>());
        }
        Object seed = template.seedData() == null ? null : template.seedData().get("technicalSpecifications");
        if (seed instanceof List<?> groups) {
            List<Map<String, Object>> seeded = new ArrayList<>();
            for (Object group : groups) {
                Map<String, Object> source = Maps.asMap(group);
                if (source == null) {
                    continue;
                }
                Map<String, Object> copy = new LinkedHashMap<>();
                copy.put("specificationId", Ids.newId("spec"));
                copy.put("title", source.get("title"));
                copy.put("badge", source.getOrDefault("badge", "PRIMARY"));
                copy.put("seeded", true);
                List<Map<String, Object>> params = new ArrayList<>();
                for (Map<String, Object> p : Maps.asMapList(source.get("parameters"))) {
                    Map<String, Object> param = new LinkedHashMap<>(p);
                    param.put("parameterId", Ids.newId("p"));
                    params.add(param);
                }
                copy.put("data", params);
                copy.put("total", source.get("parameterSlots") instanceof Number n
                        ? n.intValue() : params.size());
                seeded.add(copy);
            }
            String key = sections.containsKey("technicalSpecifications")
                    ? "technicalSpecifications" : "technicalSpecification";
            if (sections.containsKey(key)) {
                Map<String, Object> holder = new LinkedHashMap<>();
                holder.put("data", seeded);
                sections.put(key, holder);
            }
        }
        if (provided != null) {
            provided.forEach((k, v) -> sections.put(k, v));
        }
        return sections;
    }

    private List<Map<String, String>> gridColumns(FormTemplate template) {
        if (template.gridColumns() == null) {
            return List.of();
        }
        return template.gridColumns().stream()
                .map(c -> Map.of("key", c.key(), "label", c.label()))
                .toList();
    }

    /**
     * The By Grade / By Pack Size tabs are DISTINCT-VALUE counts over the same variant set, which is
     * why they do not sum to the total.
     */
    private List<Map<String, Object>> groups(List<ProductVariant> variants, String groupBy) {
        if (groupBy == null || "NONE".equalsIgnoreCase(groupBy)) {
            return List.of();
        }
        String field = switch (groupBy.toUpperCase()) {
            case "GRADE" -> "grade";
            case "PACK_SIZE" -> "packSize";
            case "ASSAY" -> "assayPurity";
            case "PARTICLE_SIZE" -> "particleSizeMesh";
            case "FORM" -> "form";
            case "FLAVOR" -> "primaryFlavor";
            case "PACKAGING_TYPE" -> "packagingType";
            case "COMBINATION" -> "variantType";
            default -> throw new ApiException(ErrorCode.VALIDATION,
                    "Unsupported groupBy '%s'.".formatted(groupBy));
        };
        Map<String, List<String>> byKey = new LinkedHashMap<>();
        for (ProductVariant variant : variants) {
            String value = detailValue(variant, field);
            if (value == null) {
                continue;
            }
            byKey.computeIfAbsent(value, k -> new ArrayList<>()).add(variant.getVariantId());
        }
        List<Map<String, Object>> out = new ArrayList<>();
        byKey.forEach((key, ids) -> out.add(Map.of("key", key, "count", ids.size(), "variantIds", ids)));
        return out;
    }

    private String detailValue(ProductVariant variant, String key) {
        Map<String, Object> details = variant.section("variantDetails");
        if (details == null) {
            return null;
        }
        Object value = details.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private ProductVariant requireVariant(ProductListing listing, String variantId) {
        return listing.getVariants().stream()
                .filter(v -> Objects.equals(v.getVariantId(), variantId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Variant " + variantId));
    }

    private FormTemplate template(ProductListing listing) {
        return templates.forListing(listing.getCategoryCode().name(), listing.getTemplateVersion());
    }

    Set<String> sectionKeys(FormTemplate template) {
        return new LinkedHashSet<>(template.childSectionKeys());
    }
}
