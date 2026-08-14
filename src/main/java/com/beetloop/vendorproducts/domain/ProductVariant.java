package com.beetloop.vendorproducts.domain;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;

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
@Entity
@Table(name = "product_variant", indexes = {
        @Index(name = "idx_product_variant_product", columnList = "product_id")
})
public class ProductVariant {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private VendorProduct product;

    @Column(name = "position", nullable = false)
    private int position;

    // ---- 3.1 Variant Details (fields present in every category's variant form) ----

    @Column(name = "name", length = 400)
    private String name;

    @Column(name = "variant_type", length = 80)
    private String variantType;

    @Column(name = "grade", length = 200)
    private String grade;

    @Column(name = "assay_purity", length = 200)
    private String assayPurity;

    @Column(name = "pack_size", length = 200)
    private String packSize;

    @Column(name = "packaging_type", length = 200)
    private String packagingType;

    @Column(name = "particle_size", length = 200)
    private String particleSize;

    @Column(name = "sku_code", length = 200)
    private String skuCode;

    @Column(name = "batch_prefix", length = 200)
    private String batchPrefix;

    @Column(name = "status", length = 40)
    private String status = "Active";

    @Type(JsonType.class)
    @Column(name = "images", columnDefinition = "text")
    private List<String> images = new ArrayList<>();

    /**
     * Variant fields that exist only for some categories — e.g. {@code flavor},
     * {@code gtin}, {@code shelfLife}, {@code markets} on Finished Goods, or
     * {@code thicknessUm}, {@code closureType}, {@code barrierLevel} on Packaging
     * Materials. Validated per category by the field registry.
     */
    @Type(JsonType.class)
    @Column(name = "details_extra", columnDefinition = "text")
    private Map<String, Object> detailsExtra = new LinkedHashMap<>();

    // ---- 3.2 Technical Specifications ----

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private List<VariantSpecificationGroup> specificationGroups = new ArrayList<>();

    // ---- 3.3 Commercial & Pricing ----

    @Embedded
    private CommercialPricing commercialPricing = new CommercialPricing();

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private List<VariantPriceTier> priceTiers = new ArrayList<>();

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private List<VariantPackagingOption> packagingOptions = new ArrayList<>();

    // ---- 3.4 Compliance & Certifications ----

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private List<VariantComplianceDocument> complianceDocuments = new ArrayList<>();

    // ---- 3.5 Search & Marketplace ----

    @Type(JsonType.class)
    @Column(name = "search_marketplace", columnDefinition = "text")
    private Map<String, Object> searchMarketplace = new LinkedHashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
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
