package com.beetloop.vendorproducts.catalogue.domain;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** T2 Commercial Master — {@code CM-…}. Intelligence QC owns this row. */
@Entity
@Table(name = "commercial_master", indexes = {
        @Index(name = "idx_t2_code", columnList = "code", unique = true),
        @Index(name = "idx_t2_status", columnList = "status"),
        @Index(name = "idx_t2_t1", columnList = "scientific_master_id"),
        @Index(name = "idx_t2_grade_key", columnList = "scientific_master_id, grade_key", unique = true)
})
public class CommercialMaster {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 40)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scientific_master_id", nullable = false)
    private ScientificMaster scientificMaster;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 16)
    private CatalogueKind kind = CatalogueKind.PRODUCT;

    @Column(name = "category", nullable = false, length = 40)
    private String category;

    @Column(name = "name", nullable = false, length = 400)
    private String name;

    @Column(name = "assay", length = 80)
    private String assay;

    @Column(name = "grade", length = 80)
    private String grade;

    @Column(name = "physical_form", length = 80)
    private String form;

    @Column(name = "origin", length = 120)
    private String origin;

    @Column(name = "colour", length = 80)
    private String colour;

    @Column(name = "source", length = 120)
    private String source;

    /** Normalised uniqueness key: assay|grade|form|origin|colour|source. */
    @Column(name = "grade_key", nullable = false, length = 500)
    private String gradeKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private CatalogueStatus status = CatalogueStatus.DRAFT;

    @Type(JsonType.class)
    @Column(name = "baseline", columnDefinition = "text")
    private Map<String, Object> baseline = new LinkedHashMap<>();

    @Column(name = "parent_code", length = 40)
    private String parentCode;

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
        refreshGradeKey();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
        refreshGradeKey();
    }

    public void refreshGradeKey() {
        this.gradeKey = normalize(assay) + "|" + normalize(grade) + "|" + normalize(form)
                + "|" + normalize(origin) + "|" + normalize(colour) + "|" + normalize(source);
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
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

    public ScientificMaster getScientificMaster() {
        return scientificMaster;
    }

    public void setScientificMaster(ScientificMaster scientificMaster) {
        this.scientificMaster = scientificMaster;
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

    public String getAssay() {
        return assay;
    }

    public void setAssay(String assay) {
        this.assay = assay;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getForm() {
        return form;
    }

    public void setForm(String form) {
        this.form = form;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getGradeKey() {
        return gradeKey;
    }

    public CatalogueStatus getStatus() {
        return status;
    }

    public void setStatus(CatalogueStatus status) {
        this.status = status;
    }

    public Map<String, Object> getBaseline() {
        return baseline;
    }

    public void setBaseline(Map<String, Object> baseline) {
        this.baseline = baseline;
    }

    public String getParentCode() {
        return parentCode;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
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
