package com.beetloop.vendorproducts.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One variant / pack size / model of a product — Step 3 of the wizard.
 *
 * <p>Holds all five "Add Variant" sub-steps: variant details (columns on this
 * entity), technical specifications ({@link VariantSpecificationGroup}),
 * commercial &amp; pricing ({@link CommercialPricing} + {@link VariantPriceTier} +
 * {@link VariantPackagingOption}), compliance &amp; certifications
 * ({@link VariantComplianceDocument}) and search &amp; marketplace
 * ({@link #searchMarketplace}).
 */
public class ProductVariant {

    @Id
    private UUID id;

    @Transient
    private VendorProduct product;

    private int position;

    // ---- 3.1 Variant Details (fields present in every category's variant form) ----

    private String name;

    private String variantType;

    private String grade;

    private String assayPurity;

    private String packSize;

    private String packagingType;

    private String particleSize;

    private String skuCode;

    private String batchPrefix;

    private String status = "Active";

    private List<String> images = new ArrayList<>();

    /**
     * Variant fields that exist only for some categories — e.g. {@code flavor},
     * {@code gtin}, {@code shelfLife}, {@code markets} on Finished Goods, or
     * {@code thicknessUm}, {@code closureType}, {@code barrierLevel} on Packaging
     * Materials. Validated per category by the field registry.
     */
    private Map<String, Object> detailsExtra = new LinkedHashMap<>();

    // ---- 3.2 Technical Specifications ----

    private List<VariantSpecificationGroup> specificationGroups = new ArrayList<>();

    // ---- 3.3 Commercial & Pricing ----

    private CommercialPricing commercialPricing = new CommercialPricing();

    private List<VariantPriceTier> priceTiers = new ArrayList<>();

    private List<VariantPackagingOption> packagingOptions = new ArrayList<>();

    // ---- 3.4 Compliance & Certifications ----

    private List<VariantComplianceDocument> complianceDocuments = new ArrayList<>();

    // ---- 3.5 Search & Marketplace ----

    private Map<String, Object> searchMarketplace = new LinkedHashMap<>();

    private Instant createdAt;

    private Instant updatedAt;

    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void replaceSpecificationGroups(List<VariantSpecificationGroup> groups) {
        this.specificationGroups.clear();
        int index = 0;
        for (VariantSpecificationGroup group : groups) {
            group.setVariant(this);
            group.setPosition(index++);
            this.specificationGroups.add(group);
        }
    }

    public void replacePriceTiers(List<VariantPriceTier> tiers) {
        this.priceTiers.clear();
        int index = 0;
        for (VariantPriceTier tier : tiers) {
            tier.setVariant(this);
            tier.setPosition(index++);
            this.priceTiers.add(tier);
        }
    }

    public void replacePackagingOptions(List<VariantPackagingOption> options) {
        this.packagingOptions.clear();
        int index = 0;
        for (VariantPackagingOption option : options) {
            option.setVariant(this);
            option.setPosition(index++);
            this.packagingOptions.add(option);
        }
    }

    public void replaceComplianceDocuments(List<VariantComplianceDocument> documents) {
        this.complianceDocuments.clear();
        int index = 0;
        for (VariantComplianceDocument document : documents) {
            document.setVariant(this);
            document.setPosition(index++);
            this.complianceDocuments.add(document);
        }
    }

    // ---- getters / setters ----

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public VendorProduct getProduct() {
        return product;
    }

    public void setProduct(VendorProduct product) {
        this.product = product;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVariantType() {
        return variantType;
    }

    public void setVariantType(String variantType) {
        this.variantType = variantType;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getAssayPurity() {
        return assayPurity;
    }

    public void setAssayPurity(String assayPurity) {
        this.assayPurity = assayPurity;
    }

    public String getPackSize() {
        return packSize;
    }

    public void setPackSize(String packSize) {
        this.packSize = packSize;
    }

    public String getPackagingType() {
        return packagingType;
    }

    public void setPackagingType(String packagingType) {
        this.packagingType = packagingType;
    }

    public String getParticleSize() {
        return particleSize;
    }

    public void setParticleSize(String particleSize) {
        this.particleSize = particleSize;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public String getBatchPrefix() {
        return batchPrefix;
    }

    public void setBatchPrefix(String batchPrefix) {
        this.batchPrefix = batchPrefix;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public Map<String, Object> getDetailsExtra() {
        return detailsExtra;
    }

    public void setDetailsExtra(Map<String, Object> detailsExtra) {
        this.detailsExtra = detailsExtra;
    }

    public List<VariantSpecificationGroup> getSpecificationGroups() {
        return specificationGroups;
    }

    public CommercialPricing getCommercialPricing() {
        return commercialPricing;
    }

    public void setCommercialPricing(CommercialPricing commercialPricing) {
        this.commercialPricing = commercialPricing;
    }

    public List<VariantPriceTier> getPriceTiers() {
        return priceTiers;
    }

    public List<VariantPackagingOption> getPackagingOptions() {
        return packagingOptions;
    }

    public List<VariantComplianceDocument> getComplianceDocuments() {
        return complianceDocuments;
    }

    public Map<String, Object> getSearchMarketplace() {
        return searchMarketplace;
    }

    public void setSearchMarketplace(Map<String, Object> searchMarketplace) {
        this.searchMarketplace = searchMarketplace;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
