package com.beetloop.vendorproducts.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Aggregate root for one vendor product listing.
 *
 * <p>Structural, cross-category data (status, ownership, listing summary, variants)
 * is modelled as real columns/relations. The two sections whose field sets differ
 * per category <em>and</em> per selected type/role card — Step 1 "Identity" and
 * Step 2 "Your Role" — are stored as JSON documents and validated on the way in
 * against the per-category schemas in
 * {@code com.beetloop.vendorproducts.registry.CategoryFieldRegistry}. That is
 * option (b) of §10.2 of the analysis spec, chosen because the identity field set
 * ranges from 24 fields (Raw Commodity) to 40 (Material Identification base) with
 * almost no overlap between categories, and the role field set changes for each of
 * the 7 role cards in every category.
 */
@Document(collection = "vendor_product")
public class VendorProduct {

    @Id
    private UUID id;

    @Indexed
    private String vendorId;

    private String createdBy;

    @Indexed
    private ProductCategory category;

    @Indexed
    private ProductStatus status = ProductStatus.DRAFT;

    /**
     * The type card chosen inside Step 1 — e.g. {@code raw-commodity} for Raw
     * Materials. Null for categories whose Step 1 has no type selector.
     */
    private String identityType;

    /** The role card chosen in Step 2 — e.g. {@code manufacturer}. */
    private String roleId;

    /** Id of the master-catalog record the vendor started from, if any. Deprecated alias of T2 code. */
    private String sourceMasterId;

    /** T2 Commercial Master FK. */
    private UUID commercialMasterId;

    /** Marketplace listing id {@code VCG-…-V###}. */
    private String listingCode;

    private boolean holdPublish;

    // ---- denormalised listing columns (drive GET /products, i.e. CatalogProduct) ----

    private String name;

    private String sku;

    private String listingCategory;

    private String originCountry;

    private String thumbEmoji;

    private String thumbImage;

    private boolean verified;

    // ---- category-specific dynamic sections ----

    private Map<String, Object> identityPayload = new LinkedHashMap<>();

    private Map<String, Object> rolePayload = new LinkedHashMap<>();

    private List<ProductVariant> variants = new ArrayList<>();

    // ---- QC ----

    private String qcReviewer;

    private String qcRemarks;

    private Instant submittedAt;

    private Instant reviewedAt;

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

    public void addVariant(ProductVariant variant) {
        variant.setProduct(this);
        variant.setPosition(this.variants.size());
        this.variants.add(variant);
    }

    public void removeVariant(ProductVariant variant) {
        this.variants.remove(variant);
        variant.setProduct(null);
    }

    // ---- getters / setters ----

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public String getIdentityType() {
        return identityType;
    }

    public void setIdentityType(String identityType) {
        this.identityType = identityType;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getSourceMasterId() {
        return sourceMasterId;
    }

    public void setSourceMasterId(String sourceMasterId) {
        this.sourceMasterId = sourceMasterId;
    }

    public UUID getCommercialMasterId() {
        return commercialMasterId;
    }

    public void setCommercialMasterId(UUID commercialMasterId) {
        this.commercialMasterId = commercialMasterId;
    }

    public String getListingCode() {
        return listingCode;
    }

    public void setListingCode(String listingCode) {
        this.listingCode = listingCode;
    }

    public boolean isHoldPublish() {
        return holdPublish;
    }

    public void setHoldPublish(boolean holdPublish) {
        this.holdPublish = holdPublish;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getListingCategory() {
        return listingCategory;
    }

    public void setListingCategory(String listingCategory) {
        this.listingCategory = listingCategory;
    }

    public String getOriginCountry() {
        return originCountry;
    }

    public void setOriginCountry(String originCountry) {
        this.originCountry = originCountry;
    }

    public String getThumbEmoji() {
        return thumbEmoji;
    }

    public void setThumbEmoji(String thumbEmoji) {
        this.thumbEmoji = thumbEmoji;
    }

    public String getThumbImage() {
        return thumbImage;
    }

    public void setThumbImage(String thumbImage) {
        this.thumbImage = thumbImage;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public Map<String, Object> getIdentityPayload() {
        return identityPayload;
    }

    public void setIdentityPayload(Map<String, Object> identityPayload) {
        this.identityPayload = identityPayload;
    }

    public Map<String, Object> getRolePayload() {
        return rolePayload;
    }

    public void setRolePayload(Map<String, Object> rolePayload) {
        this.rolePayload = rolePayload;
    }

    public List<ProductVariant> getVariants() {
        return variants;
    }

    public void setVariants(List<ProductVariant> variants) {
        this.variants = variants;
    }

    public String getQcReviewer() {
        return qcReviewer;
    }

    public void setQcReviewer(String qcReviewer) {
        this.qcReviewer = qcReviewer;
    }

    public String getQcRemarks() {
        return qcRemarks;
    }

    public void setQcRemarks(String qcRemarks) {
        this.qcRemarks = qcRemarks;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
