package com.beetloop.vendorproducts.services.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

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
@Document(collection = "vendor_service_batch")
public class VendorServiceBatch {

    @Id
    private UUID id;

    private ServiceCategory category;

    private ServiceStatus status = ServiceStatus.DRAFT;

    private String vendorId;

    private String createdBy;

    /**
     * Payloads for stages that belong to the run as a whole rather than to one
     * service — Stage 1 "Select Service", and the Compliance /
     * "Accreditations &amp; Certifications" stage where it applies to the batch.
     */
    private Map<String, Object> stagePayloads = new LinkedHashMap<>();

    private List<VendorService> items = new ArrayList<>();

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
