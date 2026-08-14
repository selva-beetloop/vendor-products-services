package com.beetloop.vendorproducts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * One certificate/document row added through the "Add Certificate / Document"
 * modal on the Compliance &amp; Certifications sub-step. Field names mirror
 * {@code CertificateDraft} / {@code DocumentRowState} in the frontend.
 */
@Entity
@Table(name = "variant_compliance_document", indexes = {
        @Index(name = "idx_compliance_doc_variant", columnList = "variant_id")
})
public class VariantComplianceDocument {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "position", nullable = false)
    private int position;

    /**
     * UI label: "Type" — Standard Certification / Additional Certification /
     * Product Document / COA &amp; Test Report.
     */
    @Column(name = "category", length = 120)
    private String category;

    @Column(name = "name", length = 400)
    private String name;

    /** UI label: "Reference No.". */
    @Column(name = "reference", length = 200)
    private String reference;

    /** UI label: "Authority". */
    @Column(name = "authority", length = 300)
    private String authority;

    @Column(name = "applicable_to", length = 400)
    private String applicableTo;

    @Column(name = "issue_date", length = 60)
    private String date;

    @Column(name = "expiry_date", length = 60)
    private String expiryDate;

    /** Active / Expiring / Expired. */
    @Column(name = "status", length = 40)
    private String status;

    @Column(name = "file_name", length = 400)
    private String fileName;

    /** Stored-file id returned by POST /uploads, when the file went through the API. */
    @Column(name = "file_id", length = 120)
    private String fileId;

    @Column(name = "file_url", length = 1000)
    private String fileUrl;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ProductVariant getVariant() {
        return variant;
    }

    public void setVariant(ProductVariant variant) {
        this.variant = variant;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
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

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getApplicableTo() {
        return applicableTo;
    }

    public void setApplicableTo(String applicableTo) {
        this.applicableTo = applicableTo;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
}
