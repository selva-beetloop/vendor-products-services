package com.beetloop.vendorproducts.pm.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * An approval item — BRD §6.6 (PM-08), ID series APR.
 *
 * <p>A view for vendor users showing, for each item, whose approval is still
 * pending. {@link #sourceCode} carries the ID of the awaiting item (an order,
 * stage or change order) as required by §12.2.
 */
public class PmApproval {

    @Id
    private UUID id;

    /** Business ID from the BRD §12.2 matrix. */
    @Indexed(unique = true)
    private String code;

    @Transient
    private PmProject project;

    private int position;

    /** ID of the item awaiting approval (ORD/STG/CO). */
    private String sourceCode;

    /** Order | Stage | Change Order. */
    private String sourceType;

    private String title;

    /** PM-08 — whose approval is still outstanding. */
    private PmEnums.PmParty pendingWith;

    private String pendingWithName;

    private PmEnums.ApprovalDecision decision;

    private Instant decidedAt;

    private String remarks;

    private java.time.LocalDate dueDate;

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

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public PmProject getProject() {
        return project;
    }

    public void setProject(PmProject project) {
        this.project = project;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public PmEnums.PmParty getPendingWith() {
        return pendingWith;
    }

    public void setPendingWith(PmEnums.PmParty pendingWith) {
        this.pendingWith = pendingWith;
    }

    public String getPendingWithName() {
        return pendingWithName;
    }

    public void setPendingWithName(String pendingWithName) {
        this.pendingWithName = pendingWithName;
    }

    public PmEnums.ApprovalDecision getDecision() {
        return decision;
    }

    public void setDecision(PmEnums.ApprovalDecision decision) {
        this.decision = decision;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public java.time.LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(java.time.LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
