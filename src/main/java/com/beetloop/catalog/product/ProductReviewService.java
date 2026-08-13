package com.beetloop.catalog.product;

import com.beetloop.catalog.product.dto.ProductDtos;
import com.beetloop.catalog.product.model.ProductListing;
import com.beetloop.catalog.product.model.ProductStepKey;
import com.beetloop.catalog.product.model.ProductVariant;
import com.beetloop.catalog.shared.error.FieldError;
import com.beetloop.catalog.shared.error.Warning;
import com.beetloop.catalog.shared.util.Maps;
import com.beetloop.catalog.template.TemplateService;
import com.beetloop.catalog.template.ValidationEngine;
import com.beetloop.catalog.template.ValidationMode;
import com.beetloop.catalog.template.ValidationResult;
import com.beetloop.catalog.template.model.FormTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The server-computed replacement for the decorative step-4 panel.
 *
 * The running UI renders every panel as "Complete" and leaves "Submit for QC" enabled on a wizard
 * with every field empty and zero variants. The flags here are real.
 */
@Service
public class ProductReviewService {

    private final ProductGuard guard;
    private final TemplateService templates;
    private final ValidationEngine validationEngine;
    private final ProductRecalculator recalculator;

    public ProductReviewService(ProductGuard guard, TemplateService templates,
                                ValidationEngine validationEngine, ProductRecalculator recalculator) {
        this.guard = guard;
        this.templates = templates;
        this.validationEngine = validationEngine;
        this.recalculator = recalculator;
    }

    public ProductDtos.ReviewResponse review(String productId) {
        ProductListing listing = guard.load(productId);
        FormTemplate template = templates.forListing(listing.getCategoryCode().name(),
                listing.getTemplateVersion());

        List<ProductDtos.StepPanel> panels = new ArrayList<>();
        List<FieldError> blocking = new ArrayList<>();
        int completed = 0;

        for (String stepKey : ProductStepKey.COMPLETABLE) {
            FormTemplate.StepSchema step = template.step(stepKey).orElse(null);
            if (step == null) {
                continue;
            }
            List<FieldError> errors = errorsFor(listing, template, step, stepKey);
            boolean complete = errors.isEmpty();
            if (complete) {
                completed++;
            }
            blocking.addAll(errors);
            panels.add(new ProductDtos.StepPanel(
                    step.key(),
                    step.pageHeading() != null ? step.pageHeading() : step.label(),
                    List.of("Required", "Buyer-visible"),
                    complete,
                    errors.size(),
                    summarise(listing, stepKey),
                    errors,
                    "/vendor/products/%s/steps/%s".formatted(listing.getId(), step.key())));
        }

        List<Warning> warnings = new ArrayList<>();
        if (recalculator.hasExpiredDocuments(listing)) {
            warnings.add(Warning.of("documents", "DOCUMENT_EXPIRED",
                    "One or more attached documents have expired and will block submission."));
        }

        return new ProductDtos.ReviewResponse(
                listing.getId(),
                listing.getCode(),
                template.label(),
                blocking.isEmpty(),
                completed,
                ProductStepKey.COMPLETABLE.size(),
                headerCard(listing),
                panels,
                blocking,
                warnings,
                "Per-variant details (technical specs, commercial pricing, compliance and marketplace) "
                        + "are managed inside each variant.");
    }

    List<FieldError> errorsFor(ProductListing listing, FormTemplate template,
                               FormTemplate.StepSchema step, String stepKey) {
        if (ProductStepKey.VARIANTS.equals(stepKey)) {
            return variantErrors(listing, template);
        }
        Map<String, Object> data = Maps.orEmpty(listing.step(stepKey));
        ValidationResult result = validationEngine.validateStep(template, step, data, data,
                ValidationMode.SUBMIT);
        return result.errors();
    }

    List<FieldError> variantErrors(ProductListing listing, FormTemplate template) {
        List<FieldError> errors = new ArrayList<>();
        if (listing.getVariants().isEmpty()) {
            errors.add(FieldError.of(ProductStepKey.VARIANTS, "variants", "Variants",
                    "AT_LEAST_ONE_VARIANT_REQUIRED", "Add at least one variant before submitting."));
            return errors;
        }
        for (ProductVariant variant : listing.getVariants()) {
            for (FormTemplate.SectionSchema section : template.childSections()) {
                String dataKey = ProductRecalculator.dataKeyOf(section);
                Map<String, Object> body = Maps.orEmpty(variant.section(dataKey));
                ValidationResult result = validationEngine.validateSection(section, body, body,
                        ValidationMode.SUBMIT, ProductStepKey.VARIANTS,
                        "variants[%s].sections.%s".formatted(variant.getVariantId(), dataKey));
                errors.addAll(result.errors());
            }
            for (Map<String, Object> row : certificateRows(variant)) {
                if ("EXPIRED".equals(row.get("status"))) {
                    errors.add(FieldError.of(ProductStepKey.VARIANTS,
                            "variants[%s].sections.complianceCertifications".formatted(variant.getVariantId()),
                            String.valueOf(row.get("name")), "DOCUMENT_EXPIRED",
                            "%s expired on %s. Replace it before submitting."
                                    .formatted(row.get("name"), row.get("expiryDate"))));
                }
            }
        }
        return errors;
    }

    private List<Map<String, Object>> certificateRows(ProductVariant variant) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String key : List.of("complianceCertifications", "certificatesAndDocuments")) {
            Map<String, Object> section = variant.section(key);
            if (section != null) {
                rows.addAll(Maps.asMapList(section.get("data")));
            }
        }
        return rows;
    }

    private Map<String, Object> headerCard(ProductListing listing) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("name", listing.getSearch().get("name"));
        card.put("origin", listing.getSearch().get("origin"));
        card.put("categoryCode", listing.getCategoryCode());
        card.put("entryPath", listing.getEntryPath());
        card.put("requestCode", listing.getRequestCode());
        return card;
    }

    /** A shallow, human-readable rendering of the step for the collapsible review panel. */
    private List<ProductDtos.SummaryItem> summarise(ProductListing listing, String stepKey) {
        List<ProductDtos.SummaryItem> items = new ArrayList<>();
        if (ProductStepKey.VARIANTS.equals(stepKey)) {
            Map<String, Object> counters = Maps.asMap(Maps.orEmpty(listing.step(stepKey)).get("counters"));
            if (counters != null) {
                counters.forEach((k, v) -> items.add(new ProductDtos.SummaryItem(k, String.valueOf(v))));
            }
            return items;
        }
        Map<String, Object> data = Maps.orEmpty(listing.step(stepKey));
        flatten(data, items, 0);
        return items.size() > 12 ? items.subList(0, 12) : items;
    }

    private void flatten(Map<String, Object> data, List<ProductDtos.SummaryItem> items, int depth) {
        if (depth > 2) {
            return;
        }
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                flatten(Maps.asMap(nested), items, depth + 1);
            } else if (!Maps.isBlank(value) && !(value instanceof List<?>)) {
                items.add(new ProductDtos.SummaryItem(entry.getKey(), String.valueOf(value)));
            }
        }
    }
}
