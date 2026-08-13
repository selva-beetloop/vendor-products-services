package com.beetloop.catalog.product;

import com.beetloop.catalog.audit.AuditEvent;
import com.beetloop.catalog.audit.AuditService;
import com.beetloop.catalog.config.CatalogProperties;
import com.beetloop.catalog.product.dto.ProductDtos;
import com.beetloop.catalog.product.model.ProductListing;
import com.beetloop.catalog.product.model.ProductStepKey;
import com.beetloop.catalog.product.model.ProductVariant;
import com.beetloop.catalog.shared.error.ApiException;
import com.beetloop.catalog.shared.error.ErrorCode;
import com.beetloop.catalog.shared.error.FieldError;
import com.beetloop.catalog.shared.error.RejectedField;
import com.beetloop.catalog.shared.error.ValidationException;
import com.beetloop.catalog.shared.error.Warning;
import com.beetloop.catalog.shared.model.EntryPath;
import com.beetloop.catalog.shared.util.Ids;
import com.beetloop.catalog.shared.util.Maps;
import com.beetloop.catalog.template.TemplateService;
import com.beetloop.catalog.template.ValidationEngine;
import com.beetloop.catalog.template.ValidationMode;
import com.beetloop.catalog.template.ValidationResult;
import com.beetloop.catalog.template.model.FormTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * OVERALL SAVE - the whole wizard in one call.
 *
 * FULL REPLACE: every step key present is written, every step key ABSENT is cleared, and every
 * variant whose id is absent from `variants` is deleted. One transaction: either the whole wizard
 * lands or none of it does.
 *
 * It is still a DRAFT write. Required fields are not enforced and qcStatus is never touched. There
 * is deliberately no `submit` flag on this endpoint - publishing needs submit-qc and a QC approval.
 */
@Service
public class ProductOverallSaveService {

    private final ProductListingRepository repository;
    private final ProductGuard guard;
    private final TemplateService templates;
    private final ValidationEngine validationEngine;
    private final ProductRecalculator recalculator;
    private final ProductDraftService draftService;
    private final CatalogProperties properties;
    private final AuditService audit;

    public ProductOverallSaveService(ProductListingRepository repository, ProductGuard guard,
                                     TemplateService templates, ValidationEngine validationEngine,
                                     ProductRecalculator recalculator, ProductDraftService draftService,
                                     CatalogProperties properties, AuditService audit) {
        this.repository = repository;
        this.guard = guard;
        this.templates = templates;
        this.validationEngine = validationEngine;
        this.recalculator = recalculator;
        this.draftService = draftService;
        this.properties = properties;
        this.audit = audit;
    }

    @Transactional
    public ProductDtos.SaveAllResponse saveAll(String pathProductId, ProductDtos.SaveAllRequest request,
                                               String ifMatch, boolean ifMatchRequired) {
        String productId = pathProductId != null ? pathProductId : request.productId();

        ProductListing listing;
        boolean created = false;
        if (productId == null) {
            listing = draftService.create(new ProductDtos.CreateProductRequest(
                    request.categoryCode(),
                    request.entryPath() == null ? EntryPath.MASTER : request.entryPath(),
                    request.masterProductId()));
            created = true;
        } else {
            listing = guard.loadEditable(productId);
            guard.checkIfMatch(listing, ifMatch, ifMatchRequired);
            if (request.categoryCode() != null && request.categoryCode() != listing.getCategoryCode()) {
                throw new ApiException(ErrorCode.CATEGORY_IMMUTABLE,
                        "This listing is %s and cannot be changed to %s."
                                .formatted(listing.getCategoryCode(), request.categoryCode()));
            }
        }

        FormTemplate template = templates.forListing(listing.getCategoryCode().name(),
                listing.getTemplateVersion());

        List<FieldError> errors = new ArrayList<>();
        List<RejectedField> rejected = new ArrayList<>();
        List<Warning> warnings = new ArrayList<>();

        List<String> savedSteps = new ArrayList<>();
        List<String> clearedSteps = new ArrayList<>();
        Map<String, Map<String, Object>> incomingSteps = request.steps() == null
                ? Map.of() : request.steps();

        for (FormTemplate.StepSchema step : template.steps()) {
            String dataKey = ProductStepSaveService.dataKey(step);
            Map<String, Object> incoming = incomingSteps.get(dataKey);
            if (incoming == null) {
                incoming = incomingSteps.get(step.key());
            }
            if (incoming == null) {
                if (!Maps.orEmpty(listing.step(dataKey)).isEmpty()) {
                    clearedSteps.add(dataKey);
                }
                listing.putStep(dataKey, new LinkedHashMap<>());
                continue;
            }
            // carryOverOtherCards = false: this endpoint replaces by contract.
            ValidationResult result = validationEngine.validateStep(template, step, incoming,
                    Maps.orEmpty(listing.step(dataKey)), ValidationMode.SAVE, false);
            errors.addAll(result.errors());
            rejected.addAll(result.rejectedFields());
            warnings.addAll(result.warnings());
            listing.putStep(dataKey, result.sanitized());
            savedSteps.add(dataKey);
        }

        ChildOutcome outcome = replaceVariants(listing, template, request.variants(), errors, rejected,
                warnings);

        if (!errors.isEmpty()) {
            throw new ValidationException(errors, warnings);
        }

        if (request.currentStep() != null && !request.currentStep().isBlank()) {
            listing.setCurrentStep(request.currentStep());
        }
        recalculator.recompute(listing, template);
        ProductListing saved = repository.save(listing);

        if (!clearedSteps.isEmpty()) {
            warnings.add(Warning.of("steps", "STEPS_CLEARED",
                    ("%d step(s) were removed because they were absent from an overall save. "
                            + "Use PUT /steps/{stepKey} to update one step without affecting the others.")
                            .formatted(clearedSteps.size())));
        }
        if (!outcome.deleted().isEmpty()) {
            warnings.add(Warning.of("variants", "VARIANTS_DELETED",
                    ("%d variant(s) were removed because they were absent from an overall save. "
                            + "Use PUT /variants/{variantId} to update one without affecting the others.")
                            .formatted(outcome.deleted().size())));
        }
        saved.getVariants().stream().filter(v -> v.getCompletionPercent() < 100)
                .forEach(v -> warnings.add(Warning.of("variants[" + v.getVariantId() + "]",
                        "VARIANT_INCOMPLETE",
                        "Variant is %d%% complete and will block submission."
                                .formatted(v.getCompletionPercent()))));

        audit.record(AuditEvent.PRODUCT_OVERALL_SAVED, "PRODUCT_LISTING", saved.getId(),
                Map.of("savedSteps", savedSteps, "clearedSteps", clearedSteps,
                        "deletedVariants", outcome.deleted(), "created", created));

        return new ProductDtos.SaveAllResponse(
                saved.getId(), saved.getCode(), saved.versionOrZero(), ProductGuard.etag(saved),
                saved.getQcStatus(), saved.getLifecycle(), Instant.now(),
                savedSteps, clearedSteps, outcome.updated(), outcome.created(), outcome.deleted(),
                saved.getCompletedSteps(), saved.getDerived(), rejected, warnings);
    }

    private record ChildOutcome(List<ProductDtos.SavedChild> updated,
                                List<ProductDtos.SavedChild> created,
                                List<String> deleted) {
    }

    private ChildOutcome replaceVariants(ProductListing listing, FormTemplate template,
                                         List<ProductDtos.VariantPayload> payloads,
                                         List<FieldError> errors, List<RejectedField> rejected,
                                         List<Warning> warnings) {
        List<ProductDtos.VariantPayload> incoming = payloads == null ? List.of() : payloads;
        int cap = properties.getLimits().getMaxVariantsPerListing();
        if (incoming.size() > cap) {
            throw new ApiException(ErrorCode.COLLECTION_CAP,
                    "A listing may hold at most %d variants; the request contained %d."
                            .formatted(cap, incoming.size()));
        }

        Map<String, ProductVariant> existingById = new LinkedHashMap<>();
        listing.getVariants().forEach(v -> existingById.put(v.getVariantId(), v));

        List<ProductVariant> rebuilt = new ArrayList<>();
        List<ProductDtos.SavedChild> updated = new ArrayList<>();
        List<ProductDtos.SavedChild> createdChildren = new ArrayList<>();
        Set<String> keptIds = new LinkedHashSet<>();
        Instant now = Instant.now();

        for (ProductDtos.VariantPayload payload : incoming) {
            ProductVariant existing = payload.variantId() == null ? null
                    : existingById.get(payload.variantId());
            boolean isNew = existing == null;
            ProductVariant variant = isNew
                    ? ProductVariant.builder().variantId(Ids.newId("var")).sections(new LinkedHashMap<>())
                            .status("DRAFT").createdAt(now).updatedAt(now).build()
                    : existing;

            Map<String, Object> sections = Maps.orEmpty(payload.sections());
            Map<String, Object> rebuiltSections = new LinkedHashMap<>();
            for (FormTemplate.SectionSchema section : template.childSections()) {
                String dataKey = ProductRecalculator.dataKeyOf(section);
                if (!sections.containsKey(dataKey)) {
                    rebuiltSections.put(dataKey, new LinkedHashMap<String, Object>());
                    continue;
                }
                ValidationResult result = validationEngine.validateSection(section,
                        Maps.orEmpty(Maps.asMap(sections.get(dataKey))),
                        Maps.orEmpty(isNew ? Map.of() : variant.section(dataKey)),
                        ValidationMode.SAVE, "variants",
                        "variants[%s].sections.%s".formatted(variant.getVariantId(), dataKey));
                errors.addAll(result.errors());
                rejected.addAll(result.rejectedFields());
                warnings.addAll(result.warnings());
                rebuiltSections.put(dataKey, result.sanitized());
            }
            variant.setSections(rebuiltSections);
            variant.setUpdatedAt(now);
            rebuilt.add(variant);
            keptIds.add(variant.getVariantId());
            if (isNew) {
                createdChildren.add(new ProductDtos.SavedChild(variant.getVariantId(), true, null));
            } else {
                updated.add(new ProductDtos.SavedChild(variant.getVariantId(), false, null));
            }
        }

        List<String> deleted = existingById.keySet().stream().filter(id -> !keptIds.contains(id)).toList();
        listing.setVariants(rebuilt);
        return new ChildOutcome(updated, createdChildren, deleted);
    }

    /** Kept for readability at the call site of the review projection. */
    public List<String> stepKeys() {
        return ProductStepKey.ALL;
    }
}
