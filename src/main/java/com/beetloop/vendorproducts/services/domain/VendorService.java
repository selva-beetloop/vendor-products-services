package com.beetloop.vendorproducts.services.domain;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
 * One service listing inside a wizard run — the row that appears in the services
 * catalog table.
 *
 * <p>Its {@link #stagePayloads} hold the per-service configuration captured in
 * the Configure Services stage, which is where the bulk of each category's fields
 * live (167 for Lab Testing, 200 for Agro-Processing, 89 for CRO's Configure
 * Commercials alone).
 *
 * <p>{@link #custom} records that the vendor created this entry themselves rather
 * than picking it from the master search. That is not cosmetic: the wizard's final
 * button flips from "Publish to Catalog" to "Submit for QC" for custom entries
 * that came through the find flow, so it drives the submit route.
 */
@Entity
@Table(name = "vendor_service", indexes = {
        @Index(name = "idx_vs_batch", columnList = "batch_id"),
        @Index(name = "idx_vs_sku", columnList = "sku")
})
public class VendorService {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private VendorServiceBatch batch;

    @Column(name = "position", nullable = false)
    private int position;

    /** T2 Commercial Master FK — services reuse the same catalogue machine. */
    @Column(name = "commercial_master_id")
    private UUID commercialMasterId;

    /** Id of the master-catalog service the vendor picked, when not a custom entry. */
    @Column(name = "source_service_id", length = 160)
    private String sourceServiceId;

    /** True for vendor-created entries (ids prefixed custom-cm-, custom-con-, custom-cro-…). */
    @Column(name = "custom", nullable = false)
    private boolean custom;

    // ---- listing columns (drive the services catalog table) ----

    @Column(name = "name", length = 400)
    private String name;

    @Column(name = "sku", length = 120)
    private String sku;

    @Column(name = "category_label", length = 200)
    private String categoryLabel;

    @Column(name = "service_type", length = 200)
    private String serviceType;

    @Column(name = "delivery_mode", length = 200)
    private String deliveryMode;

    @Column(name = "turnaround", length = 200)
    private String turnaround;

    @Column(name = "region", length = 200)
    private String region;

    @Column(name = "thumb_emoji", length = 40)
    private String thumbEmoji;

    /** configured | in-process | not-configured — the ConfigurationStatusKind badge. */
    @Column(name = "configuration_status", length = 40)
    private String configurationStatus;

    @Column(name = "rfqs", nullable = false)
    private int rfqs;

    /** Per-service stage payloads, keyed by stage key. */
    @Type(JsonType.class)
    @Column(name = "stage_payloads", columnDefinition = "text")
    private Map<String, Object> stagePayloads = new LinkedHashMap<>();

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private List<ServiceDocument> documents = new ArrayList<>();

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

    public void replaceDocuments(List<ServiceDocument> next) {
        this.documents.clear();
        int index = 0;
        for (ServiceDocument document : next) {
            document.setService(this);
            document.setPosition(index++);
            this.documents.add(document);
        }
    }

    public void addDocument(ServiceDocument document) {
        document.setService(this);
        document.setPosition(this.documents.size());
        this.documents.add(document);
    }

    public void removeDocument(ServiceDocument document) {
        this.documents.remove(document);
        document.setService(null);
    }

    // ---- getters / setters ----

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public VendorServiceBatch getBatch() {
        return batch;
    }

    public void setBatch(VendorServiceBatch batch) {
        this.batch = batch;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getSourceServiceId() {
        return sourceServiceId;
    }

    public void setSourceServiceId(String sourceServiceId) {
        this.sourceServiceId = sourceServiceId;
    }

    public UUID getCommercialMasterId() {
        return commercialMasterId;
    }

    public void setCommercialMasterId(UUID commercialMasterId) {
        this.commercialMasterId = commercialMasterId;
    }

    public boolean isCustom() {
        return custom;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
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

    public String getCategoryLabel() {
        return categoryLabel;
    }

    public void setCategoryLabel(String categoryLabel) {
        this.categoryLabel = categoryLabel;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(String deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    public String getTurnaround() {
        return turnaround;
    }

    public void setTurnaround(String turnaround) {
        this.turnaround = turnaround;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getThumbEmoji() {
        return thumbEmoji;
    }

    public void setThumbEmoji(String thumbEmoji) {
        this.thumbEmoji = thumbEmoji;
    }

    public String getConfigurationStatus() {
        return configurationStatus;
    }

    public void setConfigurationStatus(String configurationStatus) {
        this.configurationStatus = configurationStatus;
    }

    public int getRfqs() {
        return rfqs;
    }

    public void setRfqs(int rfqs) {
        this.rfqs = rfqs;
    }

    public Map<String, Object> getStagePayloads() {
        return stagePayloads;
    }

    public void setStagePayloads(Map<String, Object> stagePayloads) {
        this.stagePayloads = stagePayloads;
    }

    public List<ServiceDocument> getDocuments() {
        return documents;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
