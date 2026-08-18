package com.beetloop.vendorproducts.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import java.util.UUID;

/**
 * One certificate/document row added through the "Add Certificate / Document"
 * modal on the Compliance &amp; Certifications sub-step. Field names mirror
 * {@code CertificateDraft} / {@code DocumentRowState} in the frontend.
 */
public class VariantComplianceDocument {

    @Id
    private UUID id;

    @Transient
    private ProductVariant variant;

    private int position;

    /**
     * UI label: "Type" — Standard Certification / Additional Certification /
     * Product Document / COA &amp; Test Report.
     */
    private String category;

    private String name;

    /** UI label: "Reference No.". */
    private String reference;

    /** UI label: "Authority". */
    private String authority;

    private String applicableTo;

    private String date;

    private String expiryDate;

    /** Active / Expiring / Expired. */
    private String status;

    private String fileName;

    /** Stored-file id returned by POST /uploads, when the file went through the API. */
    private String fileId;

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
