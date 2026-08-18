package com.beetloop.vendorproducts.catalogue.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** T2 Commercial Master — {@code CM-…}. Intelligence QC owns this row. */
@Document(collection = "commercial_master")
@CompoundIndex(name = "t2_grade_key", def = "{ 'scientificMaster.$id': 1, 'gradeKey': 1 }", unique = true)
public class CommercialMaster {

    @Id
    private UUID id;

    @Indexed(unique = true)
    private String code;

    @DBRef(lazy = false)
    private ScientificMaster scientificMaster;

    /** Copied from T1 so commercial search can match CAS without a join. */
    private String scientificCasNumber;

    private String scientificName;

    private CatalogueKind kind = CatalogueKind.PRODUCT;

    private String category;

    private String name;

    private String assay;

    private String grade;

    private String form;

    private String origin;

    private String colour;

    private String source;

    /** Normalised uniqueness key: assay|grade|form|origin|colour|source. */
    private String gradeKey;

    private CatalogueStatus status = CatalogueStatus.DRAFT;

    private Map<String, Object> baseline = new LinkedHashMap<>();

    private String parentCode;

    private String qcReviewer;

    private String qcRemarks;

    private Instant reviewedAt;

    private Instant createdAt;

    private Instant updatedAt;

    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        refreshGradeKey();
    }

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

    public void setId(UUID id) {
        this.id = id;
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
        if (scientificMaster != null) {
            this.scientificCasNumber = scientificMaster.getCasNumber();
            this.scientificName = scientificMaster.getName();
        }
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
