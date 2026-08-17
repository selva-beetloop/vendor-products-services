package com.beetloop.vendorproducts.catalogue.domain;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** T1 Scientific Master — {@code SCC-…}. Intelligence QC owns this row. */
@Entity
@Table(name = "scientific_master", indexes = {
        @Index(name = "idx_t1_code", columnList = "code", unique = true),
        @Index(name = "idx_t1_status", columnList = "status"),
        @Index(name = "idx_t1_cas", columnList = "cas_number")
})
public class ScientificMaster {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 40)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 16)
    private CatalogueKind kind = CatalogueKind.PRODUCT;

    @Column(name = "category", nullable = false, length = 40)
    private String category;

    @Column(name = "name", nullable = false, length = 400)
    private String name;

    @Column(name = "cas_number", length = 80)
    private String casNumber;

    @Column(name = "formula", length = 200)
    private String formula;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private CatalogueStatus status = CatalogueStatus.DRAFT;

    @Type(JsonType.class)
    @Column(name = "payload", columnDefinition = "text")
    private Map<String, Object> payload = new LinkedHashMap<>();

    @Column(name = "qc_reviewer", length = 200)
    private String qcReviewer;

    @Column(name = "qc_remarks", length = 2000)
    private String qcRemarks;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public CatalogueKind getKind() {
        return kind;
    }

    public void setKind(CatalogueKind kind) {
        this.kind = kind;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCasNumber() {
        return casNumber;
    }

    public void setCasNumber(String casNumber) {
        this.casNumber = casNumber;
    }

    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }

    public CatalogueStatus getStatus() {
        return status;
    }

    public void setStatus(CatalogueStatus status) {
        this.status = status;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
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
