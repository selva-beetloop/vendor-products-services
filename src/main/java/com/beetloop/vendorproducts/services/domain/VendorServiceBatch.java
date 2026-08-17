package com.beetloop.vendorproducts.services.domain;

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
 * One run of the "Add Service" wizard.
 *
 * <p>Every category supports adding <em>multiple</em> services before continuing
 * (Stage 1 offers a per-row "Add" on the search results, then Configure Services
 * configures each one). The wizard run is therefore the aggregate root, and each
 * selected service is a {@link VendorService} child that ends up as its own row
 * in the services catalog.
 *
 * <p>Stage payloads are stored as JSON keyed by stage key. The field sets are far
 * too variable for columns — 1,162 fields across the five categories, from 137
 * (Consultancy) to 350 (CRO) — and they are validated on the way in against
 * {@code service-schemas.json} by
 * {@link com.beetloop.vendorproducts.services.registry.ServiceFieldRegistry}.
 * This mirrors the approach already proven in the products module.
 */
@Entity
@Table(name = "vendor_service_batch", indexes = {
        @Index(name = "idx_vsb_vendor", columnList = "vendor_id"),
        @Index(name = "idx_vsb_category", columnList = "category"),
        @Index(name = "idx_vsb_status", columnList = "status")
})
public class VendorServiceBatch {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private ServiceCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ServiceStatus status = ServiceStatus.DRAFT;

    @Column(name = "vendor_id", length = 120)
    private String vendorId;

    @Column(name = "created_by", length = 120)
    private String createdBy;

    /**
     * Payloads for stages that belong to the run as a whole rather than to one
     * service — Stage 1 "Select Service", and the Compliance /
     * "Accreditations &amp; Certifications" stage where it applies to the batch.
     */
    @Type(JsonType.class)
    @Column(name = "stage_payloads", columnDefinition = "text")
    private Map<String, Object> stagePayloads = new LinkedHashMap<>();

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private List<VendorService> items = new ArrayList<>();

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

    public void addItem(VendorService item) {
        item.setBatch(this);
        item.setPosition(this.items.size());
        this.items.add(item);
    }

    public void removeItem(VendorService item) {
        this.items.remove(item);
        item.setBatch(null);
    }

    // ---- getters / setters ----

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ServiceCategory getCategory() {
        return category;
    }

    public void setCategory(ServiceCategory category) {
        this.category = category;
    }

    public ServiceStatus getStatus() {
        return status;
    }

    public void setStatus(ServiceStatus status) {
        this.status = status;
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

    public Map<String, Object> getStagePayloads() {
        return stagePayloads;
    }

    public void setStagePayloads(Map<String, Object> stagePayloads) {
        this.stagePayloads = stagePayloads;
    }

    public List<VendorService> getItems() {
        return items;
    }

    public void setItems(List<VendorService> items) {
        this.items = items;
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
