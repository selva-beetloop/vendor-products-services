package com.beetloop.vendorproducts.pm.domain;

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
@Entity
@Table(name = "pm_approval", indexes = {@Index(name = "idx_pm_approval_code", columnList = "code"), @Index(name = "idx_pm_approval_parent", columnList = "project_id")})
public class PmApproval {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Business ID from the BRD §12.2 matrix. */
    @Column(name = "code", length = 40, nullable = false, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private PmProject project;

    @Column(name = "position", nullable = false)
    private int position;

    /** ID of the item awaiting approval (ORD/STG/CO). */
    @Column(name = "source_code", length = 40, nullable = false)
    private String sourceCode;

    /** Order | Stage | Change Order. */
    @Column(name = "source_type", length = 40)
    private String sourceType;

    @Column(name = "title", length = 400)
    private String title;

    /** PM-08 — whose approval is still outstanding. */
    @Enumerated(EnumType.STRING)
    @Column(name = "pending_with", length = 30, nullable = false)
    private PmEnums.PmParty pendingWith;

    @Column(name = "pending_with_name", length = 200)
    private String pendingWithName;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", length = 30, nullable = false)
    private PmEnums.ApprovalDecision decision;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "remarks", length = 2000)
    private String remarks;

    @Column(name = "due_date")
    private java.time.LocalDate dueDate;

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
