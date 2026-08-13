package com.beetloop.catalog.integration;

import com.beetloop.catalog.product.ProductListingRepository;
import com.beetloop.catalog.product.ProductRecalculator;
import com.beetloop.catalog.product.model.ProductCategoryCode;
import com.beetloop.catalog.product.model.ProductListing;
import com.beetloop.catalog.product.model.ProductVariant;
import com.beetloop.catalog.servicelisting.ServiceListingRepository;
import com.beetloop.catalog.servicelisting.ServiceRecalculator;
import com.beetloop.catalog.servicelisting.model.ServiceCategoryCode;
import com.beetloop.catalog.servicelisting.model.ServiceConfiguration;
import com.beetloop.catalog.servicelisting.model.ServiceListing;
import com.beetloop.catalog.shared.model.EntryPath;
import com.beetloop.catalog.shared.model.Lifecycle;
import com.beetloop.catalog.shared.model.QcStatus;
import com.beetloop.catalog.shared.util.Ids;
import com.beetloop.catalog.shared.util.SequenceService;
import com.beetloop.catalog.template.TemplateService;
import com.beetloop.catalog.template.model.FormTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gives the integrated frontend something real to render on first load.
 *
 * Opt-in and idempotent: it writes only when `beetloop.catalog.integration.demo-seed.enabled` is
 * true and the target vendor has no listings yet. It creates ordinary listings through the same
 * documents and the same recalculator the wizard uses — nothing about it is a special case that
 * the rest of the service has to know about.
 *
 * The vendor id matches the frontend's local login bypass (`TEMP-VENDOR-001`); override with
 * `beetloop.catalog.integration.demo-seed.vendor-id`.
 */
@Slf4j
@Component
public class DemoVendorCatalogSeeder implements ApplicationRunner {

    private final ProductListingRepository products;
    private final ServiceListingRepository services;
    private final TemplateService templates;
    private final ProductRecalculator productRecalculator;
    private final ServiceRecalculator serviceRecalculator;
    private final SequenceService sequences;

    @Value("${beetloop.catalog.integration.demo-seed.enabled:false}")
    private boolean enabled;

    @Value("${beetloop.catalog.integration.demo-seed.vendor-id:TEMP-VENDOR-001}")
    private String vendorId;

    public DemoVendorCatalogSeeder(ProductListingRepository products,
                                   ServiceListingRepository services, TemplateService templates,
                                   ProductRecalculator productRecalculator,
                                   ServiceRecalculator serviceRecalculator,
                                   SequenceService sequences) {
        this.products = products;
        this.services = services;
        this.templates = templates;
        this.productRecalculator = productRecalculator;
        this.serviceRecalculator = serviceRecalculator;
        this.sequences = sequences;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        if (products.countByVendorIdAndDeletedAtIsNull(vendorId) == 0) {
            seedProducts();
        }
        if (services.countByVendorIdAndDeletedAtIsNull(vendorId) == 0) {
            seedServices();
        }
    }

    // ------------------------------------------------------------------ products

    private void seedProducts() {
        product(ProductCategoryCode.RAW_MATERIALS, QcStatus.APPROVED, Lifecycle.PUBLISHED,
                identityBotanicalExtract(), roleManufacturer(),
                variant("Food Grade 25kg Drum", "CUR-FG-25KG", "Food Grade", "95%", "25 kg",
                        "HDPE Drum", 2450, "KG"));

        product(ProductCategoryCode.FINISHED_GOODS, QcStatus.PENDING_REVIEW, Lifecycle.SUBMITTED,
                identityFinishedGood(), roleBrandOwner(),
                variant("Sparkling Lemon 250 ml Can", "HYD-LEM-250", "Retail", "-", "250 ml",
                        "Aluminium Can", 42, "PCS"));

        product(ProductCategoryCode.PROCESSING_MACHINERY, null, Lifecycle.DRAFT,
                identityMachine(), roleMachineOem(),
                variant("RB-500 Servo", "RB-500-FA", "-", "-", "500 L", "-", 1850000, "PCS"));

        log.info("Integration demo seed: 3 product listings for vendor {}", vendorId);
    }

    private void product(ProductCategoryCode category, QcStatus qcStatus, Lifecycle lifecycle,
                         Map<String, Object> identity, Map<String, Object> role,
                         ProductVariant variant) {
        FormTemplate template = templates.active(category.name());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("identity", identity);
        data.put("role", role);
        data.put("variants", new LinkedHashMap<String, Object>());
        data.put("review", new LinkedHashMap<String, Object>());

        ProductListing listing = ProductListing.builder()
                .id(Ids.newId("prd"))
                .code(sequences.listingCode("PRD"))
                .vendorId(vendorId)
                .categoryCode(category)
                .categoryId(template.id())
                .entryPath(EntryPath.MASTER)
                .templateVersion(template.version())
                .currentStep("review")
                .data(data)
                .variants(new java.util.ArrayList<>(List.of(variant)))
                .qcStatus(qcStatus)
                .lifecycle(lifecycle)
                .submittedAt(qcStatus == null ? null : Instant.now())
                .publishedAt(lifecycle == Lifecycle.PUBLISHED ? Instant.now() : null)
                .build();

        productRecalculator.recompute(listing, template);
        products.save(listing);
    }

    private ProductVariant variant(String name, String sku, String grade, String assay,
                                   String packSize, String packagingType, double price,
                                   String unit) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("variantName", name);
        details.put("variantType", "COMBINATION");
        details.put("skuCode", sku);
        details.put("status", "ACTIVE");
        details.put("grade", grade);
        details.put("assayPurity", assay);
        details.put("packSize", packSize);
        details.put("packagingType", packagingType);

        Map<String, Object> base = new LinkedHashMap<>();
        base.put("pricePerUnit", java.math.BigDecimal.valueOf(price));
        base.put("unit", unit);
        base.put("moq", java.math.BigDecimal.valueOf(25));
        base.put("leadTime", "7-10 days");

        Map<String, Object> sample = new LinkedHashMap<>();
        sample.put("sampleAvailable", true);
        sample.put("freeOrPaidSample", "FREE");
        sample.put("sampleTurnaround", "3 days");

        Map<String, Object> pricing = new LinkedHashMap<>();
        pricing.put("pricingQuantity", Map.of("basePricing", base));
        pricing.put("commercialTradeTerms",
                Map.of("commercialDetails", Map.of("currency", "INR", "incoterms", "FOB")));
        pricing.put("packagingAndSamples", Map.of("sampleInformation", sample));

        Map<String, Object> marketplace = new LinkedHashMap<>();
        marketplace.put("searchTagsAndKeywords", List.of(name.toLowerCase()));
        marketplace.put("marketplaceListing", Map.of("listingTitle", name));

        Map<String, Object> sections = new LinkedHashMap<>();
        sections.put("variantDetails", details);
        sections.put("commercialPricing", pricing);
        sections.put("searchMarketplace", marketplace);

        Instant now = Instant.now();
        return ProductVariant.builder()
                .variantId(Ids.newId("var"))
                .sections(sections)
                .status("ACTIVE")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private Map<String, Object> identityBotanicalExtract() {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("extractName", "Curcumin 95% Botanical Extract");
        card.put("botanicalName", "Curcuma longa");
        card.put("plantPartUsed", "RHIZOME");
        card.put("extractType", "STANDARDIZED");
        card.put("extractionMethod", "SOLVENT");
        card.put("markerCompound", "Curcuminoids");
        card.put("markerAssayStrength", "95%");
        card.put("standardizationType", "BY_HPLC");
        card.put("countryOfOrigin", "IN");

        Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("materialType", "BOTANICAL_EXTRACT");
        identity.put("botanicalExtract", card);
        identity.put("productDescription",
                "Standardised turmeric rhizome extract, 95% curcuminoids by HPLC.");
        return identity;
    }

    private Map<String, Object> identityFinishedGood() {
        Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("productType", "BRANDED_RETAIL_READY");
        identity.put("brandName", "HydrateX");
        identity.put("commercialProductName", "Sparkling Lemon Energy Water");
        identity.put("industry", "BEVERAGE");
        identity.put("sector", "FUNCTIONAL_BEVERAGE");
        identity.put("category", "RTD");
        identity.put("commercialStage", "COMMERCIAL");
        identity.put("countryOfOrigin", "IN");
        identity.put("shortDescription", "Zero-sugar sparkling electrolyte water with natural lemon.");
        return identity;
    }

    private Map<String, Object> identityMachine() {
        Map<String, Object> basic = new LinkedHashMap<>();
        basic.put("machineName", "Ribbon Blender RB-500");
        basic.put("commercialModelSeries", "RB-500");
        basic.put("brandOem", "MixTech");
        basic.put("machineCategory", "MIXING_BLENDING");
        basic.put("subCategory", "RIBBON_BLENDER");
        basic.put("machineType", "BATCH");
        basic.put("countryOfManufacture", "IN");

        Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("basicInformation", basic);
        return identity;
    }

    private Map<String, Object> roleManufacturer() {
        Map<String, Object> role = new LinkedHashMap<>();
        role.put("supplyRole", "MANUFACTURER");
        role.put("manufacturer", Map.of("facilityId", "fac_demo_1",
                "legalEntityName", "Spice Labs Pvt Ltd"));
        return role;
    }

    private Map<String, Object> roleBrandOwner() {
        Map<String, Object> role = new LinkedHashMap<>();
        role.put("vendorRole", "BRAND_OWNER_OBM");
        role.put("brandOwnerObm", Map.of("brandName", "HydrateX",
                "countryOfBrandOwnership", "IN"));
        return role;
    }

    private Map<String, Object> roleMachineOem() {
        Map<String, Object> role = new LinkedHashMap<>();
        role.put("businessRole", "MANUFACTURER_OEM");
        role.put("manufacturerOem", Map.of("registeredCompanyName", "MixTech Engineering",
                "countryOfBusinessRegistration", "IN"));
        return role;
    }

    // ------------------------------------------------------------------ services

    private void seedServices() {
        service(ServiceCategoryCode.LAB_TESTING, QcStatus.APPROVED, Lifecycle.PUBLISHED,
                "Heavy Metals by ICP-MS", "INDIVIDUAL_TEST", 4500);
        service(ServiceCategoryCode.CONSULTANCY, null, Lifecycle.DRAFT,
                "FSSAI Licensing & Compliance Advisory", "INDIVIDUAL_SERVICE", 85000);
        log.info("Integration demo seed: 2 service listings for vendor {}", vendorId);
    }

    private void service(ServiceCategoryCode category, QcStatus qcStatus, Lifecycle lifecycle,
                         String name, String serviceType, double price) {
        FormTemplate template = templates.active(category.name());
        String selectionId = Ids.newId("sel");

        Map<String, Object> selected = new LinkedHashMap<>();
        selected.put("selectionId", selectionId);
        selected.put("name", name);
        selected.put("serviceType", serviceType);
        selected.put("source", "COMMERCIAL_MASTER");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("selectService", new LinkedHashMap<>(Map.of(
                "entryPath", "MASTER", "selectedServices", List.of(selected))));
        data.put("configureServices", new LinkedHashMap<String, Object>());
        data.put("compliance", new LinkedHashMap<String, Object>());
        data.put("review", new LinkedHashMap<String, Object>());

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("serviceClassification", Map.of("serviceName", name,
                "commercialDisplayName", name, "serviceType", serviceType));
        details.put("serviceDefinition", Map.of("shortDescription", name,
                "typicalTurnaroundTime", "5 working days"));

        Map<String, Object> sections = new LinkedHashMap<>();
        sections.put("serviceDetails", details);
        sections.put("marketplaceAndSearch", Map.of("marketplaceListing",
                Map.of("listingTitle", name)));

        Instant now = Instant.now();
        ServiceConfiguration configuration = ServiceConfiguration.builder()
                .configurationId(Ids.newId("cfg"))
                .selectionId(selectionId)
                .name(name)
                .serviceType(serviceType)
                .source("COMMERCIAL_MASTER")
                .sections(sections)
                .createdAt(now)
                .updatedAt(now)
                .build();

        ServiceListing listing = ServiceListing.builder()
                .id(Ids.newId("svc"))
                .code(sequences.listingCode("SVC"))
                .vendorId(vendorId)
                .categoryCode(category)
                .categoryId(template.id())
                .entryPath(EntryPath.MASTER)
                .templateVersion(template.version())
                .currentStep("review")
                .data(data)
                .configurations(new java.util.ArrayList<>(List.of(configuration)))
                .qcStatus(qcStatus)
                .lifecycle(lifecycle)
                .submittedAt(qcStatus == null ? null : now)
                .publishedAt(lifecycle == Lifecycle.PUBLISHED ? now : null)
                .build();

        serviceRecalculator.recompute(listing, template);
        listing.getSearch().put("price", java.math.BigDecimal.valueOf(price));
        services.save(listing);
    }
}
