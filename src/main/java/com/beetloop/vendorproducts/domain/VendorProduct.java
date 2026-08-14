package com.beetloop.vendorproducts.domain;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Entity
@Table(name = "vendor_product", indexes = {
        @Index(name = "idx_vendor_product_vendor", columnList = "vendor_id"),
        @Index(name = "idx_vendor_product_category", columnList = "category"),
        @Index(name = "idx_vendor_product_status", columnList = "status")
})
public class VendorProduct {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "vendor_id", length = 120)
    private String vendorId;

    @Column(name = "created_by", length = 120)
    private String createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private ProductCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ProductStatus status = ProductStatus.DRAFT;

    /**
     * The type card chosen inside Step 1 — e.g. {@code raw-commodity} for Raw
     * Materials. Null for categories whose Step 1 has no type selector.
     */
    @Column(name = "identity_type", length = 80)
    private String identityType;

    /** The role card chosen in Step 2 — e.g. {@code manufacturer}. */
    @Column(name = "role_id", length = 80)
    private String roleId;

    /** Id of the master-catalog record the vendor started from, if any. */
    @Column(name = "source_master_id", length = 120)
    private String sourceMasterId;

    // ---- denormalised listing columns (drive GET /products, i.e. CatalogProduct) ----

    @Column(name = "name", length = 400)
    private String name;

    @Column(name = "sku", length = 120)
    private String sku;

    @Column(name = "listing_category", length = 200)
    private String listingCategory;

    @Column(name = "origin_country", length = 120)
    private String originCountry;

    @Column(name = "thumb_emoji", length = 40)
    private String thumbEmoji;

    @Column(name = "thumb_image", length = 1000)
    private String thumbImage;

    @Column(name = "verified")
    private boolean verified;

    // ---- category-specific dynamic sections ----

    @Type(JsonType.class)
    @Column(name = "identity_payload", columnDefinition = "text")
    private Map<String, Object> identityPayload = new LinkedHashMap<>();

    @Type(JsonType.class)
    @Column(name = "role_payload", columnDefinition = "text")
    private Map<String, Object> rolePayload = new LinkedHashMap<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private List<ProductVariant> variants = new ArrayList<>();

    // ---- QC ----

    @Column(name = "qc_reviewer", length = 200)
    private String qcReviewer;

    @Column(name = "qc_remarks", length = 2000)
    private String qcRemarks;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

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
