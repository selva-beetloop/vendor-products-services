package com.beetloop.catalog.product;

import com.beetloop.catalog.product.model.ProductCategoryCode;
import com.beetloop.catalog.product.model.ProductListing;
import com.beetloop.catalog.product.model.ProductStepKey;
import com.beetloop.catalog.product.model.ProductVariant;
import com.beetloop.catalog.shared.util.Maps;
import com.beetloop.catalog.template.DerivationService;
import com.beetloop.catalog.template.ValidationEngine;
import com.beetloop.catalog.template.model.FormTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runs after every write. Recomputes every derived value on a product listing, so that
 * "reject client values for derived fields" is enforceable rather than aspirational.
 */
@Component
public class ProductRecalculator {

    private final ValidationEngine validationEngine;
    private final DerivationService derivation;

    public ProductRecalculator(ValidationEngine validationEngine, DerivationService derivation) {
        this.validationEngine = validationEngine;
        this.derivation = derivation;
    }

    public void recompute(ProductListing listing, FormTemplate template) {
        recomputeVariants(listing, template);
        recomputeVariantsStep(listing);
        recomputeCompletedSteps(listing, template);
        recomputeDerived(listing);
        recomputeSearchProjection(listing);
    }

    // ------------------------------------------------------------------ variants

    private void recomputeVariants(ProductListing listing, FormTemplate template) {
        List<FormTemplate.SectionSchema> sections = template.childSections() == null
                ? List.of() : template.childSections();
        for (ProductVariant variant : listing.getVariants()) {
            if (variant.getSections() == null) {
                variant.setSections(new LinkedHashMap<>());
            }
            int complete = 0;
            for (FormTemplate.SectionSchema section : sections) {
                String dataKey = dataKeyOf(section);
                Map<String, Object> body = variant.section(dataKey);
                if (body == null) {
                    continue;
                }
                deriveSection(dataKey, body, variant);
                if (validationEngine.isSectionComplete(section, body)) {
                    complete++;
                }
            }
            variant.setCompletionPercent(derivation.percent(complete, Math.max(sections.size(), 1)));

            Map<String, Object> details = variant.section("variantDetails");
            if (details != null && !Maps.isBlank(details.get("status"))) {
                variant.setStatus(String.valueOf(details.get("status")));
            }
        }
    }

    /** Dispatch to the right derivation for a variant stage. */
    public void deriveSection(String dataKey, Map<String, Object> body, ProductVariant variant) {
        switch (dataKey) {
            case "technicalSpecifications", "technicalSpecification" ->
                    derivation.deriveSpecificationGroups(body, null);
            case "complianceCertifications", "certificatesAndDocuments" ->
                    derivation.deriveCertificateRows(body);
            case "constructionSpecification" -> derivation.deriveConstructionTotals(body);
            case "variantDetails" -> {
                String sku = derivation.variantCodeSku(body);
                if (sku != null && body.containsKey("variantCodeSku")) {
                    body.put("variantCodeSku", sku);
                }
            }
            default -> {
                // no derivation for this stage
            }
        }
    }

    /** Step 3 carries listing-level grouping and counters only; the rows live in variants[]. */
    private void recomputeVariantsStep(ProductListing listing) {
        Map<String, Object> step = listing.step(ProductStepKey.VARIANTS);
        if (step == null) {
            step = new LinkedHashMap<>();
            listing.putStep(ProductStepKey.VARIANTS, step);
        }
        List<Map<String, Object>> details = new ArrayList<>();
        for (ProductVariant variant : listing.getVariants()) {
            Map<String, Object> d = variant.section("variantDetails");
            if (d != null) {
                details.add(d);
            }
        }
        step.put("counters", derivation.variantCounters(details));
        step.put("groupings", derivation.variantGroupings(details));
        step.put("pagination", derivation.pagination(listing.getVariants().size()));
    }

    // ------------------------------------------------------------------ completeness

    private void recomputeCompletedSteps(ProductListing listing, FormTemplate template) {
        List<String> completed = new ArrayList<>();
        for (String stepKey : ProductStepKey.COMPLETABLE) {
            if (isStepComplete(listing, template, stepKey)) {
                completed.add(stepKey);
            }
        }
        listing.setCompletedSteps(completed);
    }

    public boolean isStepComplete(ProductListing listing, FormTemplate template, String stepKey) {
        if (ProductStepKey.VARIANTS.equals(stepKey)) {
            return !listing.getVariants().isEmpty()
                    && listing.getVariants().stream().allMatch(v -> v.getCompletionPercent() >= 100);
        }
        return template.step(stepKey)
                .map(step -> validationEngine.isStepComplete(template, step, listing.step(stepKey)))
                .orElse(false);
    }

    // ------------------------------------------------------------------ derived + search

    private void recomputeDerived(ProductListing listing) {
        Map<String, Object> derived = new LinkedHashMap<>();
        derived.put("variantCount", listing.getVariants().size());

        Map<String, Object> variantsStep = listing.step(ProductStepKey.VARIANTS);
        if (variantsStep != null) {
            derived.put("counters", variantsStep.get("counters"));
            derived.put("groupings", variantsStep.get("groupings"));
        }

        int documents = 0;
        int expired = 0;
        for (ProductVariant variant : listing.getVariants()) {
            for (String key : List.of("complianceCertifications", "certificatesAndDocuments")) {
                Map<String, Object> section = variant.section(key);
                if (section == null) {
                    continue;
                }
                for (Map<String, Object> row : Maps.asMapList(section.get("data"))) {
                    documents++;
                    if ("EXPIRED".equals(row.get("status"))) {
                        expired++;
                    }
                }
            }
        }
        derived.put("documentCount", documents);
        derived.put("expiredDocumentCount", expired);

        int totalSteps = ProductStepKey.COMPLETABLE.size();
        int variantContribution = listing.getVariants().isEmpty() ? 0
                : (int) listing.getVariants().stream().mapToInt(ProductVariant::getCompletionPercent).average()
                        .orElse(0);
        int stepContribution = derivation.percent(listing.getCompletedSteps().size(), totalSteps);
        derived.put("completionPercent", (stepContribution * 2 + variantContribution) / 3);

        listing.setDerived(derived);
    }

    /**
     * The catalog grid needs eleven columns and six filters over data that lives at different depths
     * per category. Querying that directly means a different projection per category and no usable
     * index, so it is denormalised here on every write.
     */
    private void recomputeSearchProjection(ProductListing listing) {
        Map<String, Object> search = new LinkedHashMap<>();
        Map<String, Object> identity = listing.step(ProductStepKey.IDENTITY);
        Map<String, Object> role = listing.step(ProductStepKey.ROLE);

        search.put("name", displayName(listing, identity));
        search.put("skuCode", firstVariantValue(listing, "skuCode"));
        search.put("functionalRolePrimary", roleCode(role));
        search.put("origin", originOf(identity));
        search.put("categoryCode", listing.getCategoryCode() == null ? null
                : listing.getCategoryCode().name());
        search.put("sampleAvailable", sampleAvailable(listing));
        search.put("quickPricing", quickPricing(listing));
        search.put("keywords", keywords(listing, identity));
        search.put("expiredDocuments", listing.getDerived().get("expiredDocumentCount"));
        listing.setSearch(search);
    }

    private String displayName(ProductListing listing, Map<String, Object> identity) {
        if (identity == null) {
            return null;
        }
        // Each category names its product differently; take the first that is populated.
        for (String key : List.of("commercialProductName", "productName", "machineName", "brandName")) {
            if (!Maps.isBlank(identity.get(key))) {
                return String.valueOf(identity.get(key));
            }
        }
        Map<String, Object> basic = Maps.mapAt(identity, "basicInformation");
        if (basic == null) {
            basic = Maps.mapAt(identity, "basicMachineInformation");
        }
        if (basic != null && !Maps.isBlank(basic.get("machineName"))) {
            return String.valueOf(basic.get("machineName"));
        }
        // Raw Materials: the name lives inside the selected type card.
        for (Object value : identity.values()) {
            Map<String, Object> card = Maps.asMap(value);
            if (card == null) {
                continue;
            }
            for (String key : List.of("extractName", "commodityName", "compoundName",
                    "ingredientNameInciCommon", "enzymeNameCommon", "vitaminNameCommon",
                    "additiveNameCommon")) {
                if (!Maps.isBlank(card.get(key))) {
                    return String.valueOf(card.get(key));
                }
            }
            Map<String, Object> productIdentity = Maps.mapAt(card, "productIdentity");
            if (productIdentity != null && !Maps.isBlank(productIdentity.get("compoundName"))) {
                return String.valueOf(productIdentity.get("compoundName"));
            }
        }
        return null;
    }

    private String roleCode(Map<String, Object> role) {
        if (role == null) {
            return null;
        }
        for (String key : List.of("supplyRole", "businessRole", "vendorRole")) {
            if (!Maps.isBlank(role.get(key))) {
                return String.valueOf(role.get(key));
            }
        }
        Map<String, Object> ownership = Maps.mapAt(role, "ownership");
        return ownership == null ? null : Maps.str(ownership, "ownershipType");
    }

    private String originOf(Map<String, Object> identity) {
        if (identity == null) {
            return null;
        }
        for (String key : List.of("countryOfOrigin", "countryOfManufacture")) {
            if (!Maps.isBlank(identity.get(key))) {
                return String.valueOf(identity.get(key));
            }
        }
        for (Object value : identity.values()) {
            Map<String, Object> card = Maps.asMap(value);
            if (card != null && !Maps.isBlank(card.get("countryOfOrigin"))) {
                return String.valueOf(card.get("countryOfOrigin"));
            }
        }
        return null;
    }

    private Object firstVariantValue(ProductListing listing, String key) {
        for (ProductVariant variant : listing.getVariants()) {
            Map<String, Object> details = variant.section("variantDetails");
            if (details != null && !Maps.isBlank(details.get(key))) {
                return details.get(key);
            }
        }
        return null;
    }

    private boolean sampleAvailable(ProductListing listing) {
        for (ProductVariant variant : listing.getVariants()) {
            Map<String, Object> pricing = variant.section("commercialPricing");
            if (pricing == null) {
                pricing = variant.section("commercialsAndPricing");
            }
            if (pricing == null) {
                continue;
            }
            Map<String, Object> packaging = Maps.mapAt(pricing, "packagingAndSamples");
            Map<String, Object> sample = packaging == null ? null
                    : Maps.mapAt(packaging, "sampleInformation");
            if (sample != null && Boolean.TRUE.equals(sample.get("sampleAvailable"))) {
                return true;
            }
        }
        // Packaging Materials keeps commercial terms at listing level.
        Map<String, Object> role = listing.step(ProductStepKey.ROLE);
        Map<String, Object> terms = role == null ? null : Maps.mapAt(role, "commercialTerms");
        return terms != null && Boolean.TRUE.equals(terms.get("samplesAvailableOnRequest"));
    }

    private Map<String, Object> quickPricing(ProductListing listing) {
        for (ProductVariant variant : listing.getVariants()) {
            Map<String, Object> pricing = variant.section("commercialPricing");
            if (pricing == null) {
                pricing = variant.section("commercialsAndPricing");
            }
            if (pricing == null) {
                continue;
            }
            Map<String, Object> quantity = Maps.mapAt(pricing, "pricingQuantity");
            Map<String, Object> base = quantity == null ? null : Maps.mapAt(quantity, "basePricing");
            Map<String, Object> terms = Maps.mapAt(pricing, "commercialTradeTerms");
            Map<String, Object> commercial = terms == null ? null : Maps.mapAt(terms, "commercialDetails");
            if (base != null && base.get("pricePerUnit") != null) {
                Map<String, Object> money = new LinkedHashMap<>();
                money.put("amount", String.valueOf(base.get("pricePerUnit")));
                money.put("currency", commercial == null ? "INR"
                        : String.valueOf(commercial.getOrDefault("currency", "INR")));
                money.put("unit", base.get("unit"));
                return money;
            }
        }
        return null;
    }

    private List<String> keywords(ProductListing listing, Map<String, Object> identity) {
        Set<String> keywords = new LinkedHashSet<>();
        for (ProductVariant variant : listing.getVariants()) {
            Map<String, Object> marketplace = variant.section("searchMarketplace");
            if (marketplace == null) {
                marketplace = variant.section("marketplaceAndSearch");
            }
            if (marketplace == null) {
                continue;
            }
            addAll(keywords, marketplace.get("searchTagsAndKeywords"));
            addAll(keywords, marketplace.get("synonymsOrAlternativeNames"));
            Map<String, Object> indexing = Maps.mapAt(marketplace, "searchAndIndexing");
            if (indexing != null) {
                addAll(keywords, indexing.get("searchTagsAndKeywords"));
            }
        }
        String name = displayName(listing, identity);
        if (name != null) {
            keywords.add(name.toLowerCase());
        }
        return new ArrayList<>(keywords);
    }

    private void addAll(Set<String> target, Object raw) {
        List<Object> list = Maps.asList(raw);
        if (list == null) {
            return;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                Object value = m.get("value");
                if (value != null) {
                    target.add(String.valueOf(value).toLowerCase());
                }
            } else if (!Maps.isBlank(item)) {
                target.add(String.valueOf(item).toLowerCase());
            }
        }
    }

    public static String dataKeyOf(FormTemplate.SectionSchema section) {
        return section.dataKey() != null ? section.dataKey()
                : com.beetloop.catalog.shared.util.Keys.toCamel(section.key());
    }

    /** True when any linked or embedded document has already expired - blocks submit. */
    public boolean hasExpiredDocuments(ProductListing listing) {
        Object expired = listing.getDerived().get("expiredDocumentCount");
        return expired instanceof Number n && n.intValue() > 0;
    }

    public LocalDate today() {
        return LocalDate.now();
    }

    public boolean isMachinery(ProductCategoryCode code) {
        return code == ProductCategoryCode.PROCESSING_MACHINERY
                || code == ProductCategoryCode.PACKAGING_MACHINERY;
    }
}
