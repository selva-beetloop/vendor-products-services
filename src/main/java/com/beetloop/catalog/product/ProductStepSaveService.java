package com.beetloop.catalog.product;

import com.beetloop.catalog.audit.AuditEvent;
import com.beetloop.catalog.audit.AuditService;
import com.beetloop.catalog.product.dto.ProductDtos;
import com.beetloop.catalog.product.model.ProductListing;
import com.beetloop.catalog.product.model.ProductStepKey;
import com.beetloop.catalog.shared.error.RejectedField;
import com.beetloop.catalog.shared.error.ValidationException;
import com.beetloop.catalog.shared.error.Warning;
import com.beetloop.catalog.shared.tenant.TenantContext;
import com.beetloop.catalog.shared.util.Keys;
import com.beetloop.catalog.shared.util.Maps;
import com.beetloop.catalog.template.TemplateService;
import com.beetloop.catalog.template.ValidationEngine;
import com.beetloop.catalog.template.ValidationMode;
import com.beetloop.catalog.template.ValidationResult;
import com.beetloop.catalog.template.model.FormTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * STEP-WISE SAVE.
 *
 * The write path behind every "Save & Continue", "Save as Draft", "Save & Continue Later" and
 * "Continue to Specifications" button in the product wizard - all four labels are the same operation.
 *
 * Replaces data.{stepKey} whole; sibling step keys are untouched. Partial data is accepted and
 * required fields are NOT enforced. Never changes qcStatus.
 */
@Service
public class ProductStepSaveService {

    private final ProductListingRepository repository;
    private final ProductGuard guard;
    private final TemplateService templates;
    private final ValidationEngine validationEngine;
    private final ProductRecalculator recalculator;
    private final AuditService audit;

    public ProductStepSaveService(ProductListingRepository repository, ProductGuard guard,
                                  TemplateService templates, ValidationEngine validationEngine,
                                  ProductRecalculator recalculator, AuditService audit) {
        this.repository = repository;
        this.guard = guard;
        this.templates = templates;
        this.validationEngine = validationEngine;
        this.recalculator = recalculator;
        this.audit = audit;
    }

    public ProductDtos.StepReadResponse read(String productId, String stepKey) {
        ProductListing listing = guard.load(productId);
        FormTemplate template = template(listing);
        FormTemplate.StepSchema step = templates.requireStep(template, stepKey);
        String dataKey = dataKey(step);
        return new ProductDtos.StepReadResponse(
                listing.getId(), step.key(), listing.getTemplateVersion(),
                recalculator.isStepComplete(listing, template, dataKey),
                Maps.orEmpty(listing.step(dataKey)));
    }

    public ProductDtos.StepSaveResponse save(String productId, String stepKey,
                                             ProductDtos.StepSaveRequest request, String ifMatch) {
        ProductListing listing = guard.loadEditable(productId);
        guard.checkIfMatch(listing, ifMatch, false);

        FormTemplate template = template(listing);
        FormTemplate.StepSchema step = templates.requireStep(template, stepKey);
        String dataKey = dataKey(step);

        Map<String, Object> existing = Maps.orEmpty(listing.step(dataKey));
        ValidationResult result = validationEngine.validateStep(
                template, step, Maps.orEmpty(request.data()), existing, ValidationMode.SAVE);
        // Save is permissive about completeness but strict about coherence.
        result.throwIfInvalid();

        listing.putStep(dataKey, result.sanitized());
        if (request.currentStep() != null && !request.currentStep().isBlank()) {
            listing.setCurrentStep(request.currentStep());
        }
        recordAttestation(listing, dataKey, result.sanitized());
        recalculator.recompute(listing, template);
        ProductListing saved = repository.save(listing);

        audit.record(AuditEvent.PRODUCT_STEP_SAVED, "PRODUCT_LISTING", saved.getId(),
                Map.of("stepKey", step.key(),
                        "toVersion", saved.versionOrZero(),
                        "rejectedFields", result.rejectedFields().stream()
                                .map(RejectedField::path).toList()));

        List<Warning> warnings = new ArrayList<>(result.warnings());
        warnings.addAll(stepWarnings(saved, dataKey));

        return new ProductDtos.StepSaveResponse(
                saved.getId(), step.key(), saved.versionOrZero(), ProductGuard.etag(saved),
                saved.getCurrentStep(), saved.getCompletedSteps(), saved.getQcStatus(), Instant.now(),
                Maps.orEmpty(saved.step(dataKey)), result.rejectedFields(), warnings);
    }

    /**
     * Dry run. Writes nothing, and returns what submit-qc WOULD say about this step - which is what
     * the categories that block client-side should be driving their messages from.
     */
    public ProductDtos.ValidateStepResponse validate(String productId, String stepKey,
                                                     ProductDtos.StepSaveRequest request) {
        ProductListing listing = guard.load(productId);
        FormTemplate template = template(listing);
        FormTemplate.StepSchema step = templates.requireStep(template, stepKey);

        ValidationResult result = validationEngine.validateStep(template, step,
                Maps.orEmpty(request.data()), Maps.orEmpty(listing.step(dataKey(step))),
                ValidationMode.SUBMIT);

        return new ProductDtos.ValidateStepResponse(
                step.key(), result.valid(), result.valid(),
                result.valid() ? null : ValidationException.DEFAULT_BANNER,
                result.errors(), result.warnings());
    }

    private List<Warning> stepWarnings(ProductListing listing, String dataKey) {
        List<Warning> warnings = new ArrayList<>();
        if (ProductStepKey.VARIANTS.equals(dataKey) && listing.getVariants().isEmpty()) {
            warnings.add(Warning.of("variants", "NO_VARIANTS",
                    "No variants yet. At least one is required before submitting."));
        }
        listing.getVariants().stream()
                .filter(v -> v.getCompletionPercent() < 100)
                .forEach(v -> warnings.add(Warning.of("variants[" + v.getVariantId() + "]",
                        "VARIANT_INCOMPLETE",
                        "Variant %s is %d%% complete and will block submission."
                                .formatted(v.getVariantId(), v.getCompletionPercent()))));
        return warnings;
    }

    /**
     * The Packaging Materials authorisation checkbox is a legal acknowledgement, not a preference.
     * A boolean in a mutable document is not evidence, so it also becomes an append-only audit event.
     */
    private void recordAttestation(ProductListing listing, String dataKey, Map<String, Object> data) {
        if (!ProductStepKey.ROLE.equals(dataKey)) {
            return;
        }
        Map<String, Object> ownership = Maps.mapAt(data, "ownership");
        if (ownership == null || !Boolean.TRUE.equals(ownership.get("authorizedToListAndSell"))) {
            return;
        }
        Map<String, Object> attestation = new HashMap<>();
        attestation.put("recordedAt", Instant.now().toString());
        attestation.put("recordedBy", TenantContext.userId());
        ownership.put("attestation", attestation);
        audit.record(AuditEvent.ATTESTATION_RECORDED, "PRODUCT_LISTING", listing.getId(),
                Map.of("field", "data.role.ownership.authorizedToListAndSell",
                        "statement", "I am authorized to list and sell this packaging material on Beetloop"));
    }

    FormTemplate template(ProductListing listing) {
        return templates.forListing(listing.getCategoryCode().name(), listing.getTemplateVersion());
    }

    static String dataKey(FormTemplate.StepSchema step) {
        return step.dataKey() != null ? step.dataKey() : Keys.toCamel(step.key());
    }
}
