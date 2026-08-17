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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A change order — BRD §6.5 (PM-06/PM-07), ID series CO.
 *
 * <p>Requires counterparty approval: a vendor-raised CO is approved by the buyer
 * and vice versa, so {@link #raisedBy} drives who must act. PM-07 also requires
 * the raiser to be visible on screen. An approved change order cannot re-open a
 * stage already completed and paid — enforced in the service layer.
 *
 * <p>BP-23: when a buyer requests rework the system auto-raises a CO of type
 * {@code REWORK} whose approval is the buyer's pay-extra checkbox, recorded in
 * {@link #autoRaised} and {@link #payExtraAgreed}.
 */
@Entity
@Table(name = "pm_change_order", indexes = {@Index(name = "idx_pm_change_order_code", columnList = "code"), @Index(name = "idx_pm_change_order_parent", columnList = "project_id")})
public class PmChangeOrder {

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

    /** Order the change applies to. */
    @Column(name = "order_code", length = 40)
    private String orderCode;

    @Column(name = "title", length = 400, nullable = false)
    private String title;

    @Column(name = "description", length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", length = 30, nullable = false)
    private PmEnums.ChangeOrderType changeType;

    /** PM-07 — raiser visibility is required on both views. */
    @Enumerated(EnumType.STRING)
    @Column(name = "raised_by", length = 30, nullable = false)
    private PmEnums.PmParty raisedBy;

    @Column(name = "raised_by_name", length = 200)
    private String raisedByName;

    /** Counterparty who must approve. */
    @Enumerated(EnumType.STRING)
    @Column(name = "approver_party", length = 30)
    private PmEnums.PmParty approverParty;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", length = 30, nullable = false)
    private PmEnums.ApprovalDecision decision;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decision_remarks", length = 2000)
    private String decisionRemarks;

    /** Added to the stage payment request once approved (BP-23). */
    @Column(name = "amount", precision = 19, scale = 2)
    private java.math.BigDecimal amount;

    /** True for the system-raised Rework CO. */
    @Column(name = "auto_raised", nullable = false)
    private boolean autoRaised;

    /** Buyer's "I will agree to pay extra for rework" checkbox. */
    @Column(name = "pay_extra_agreed", nullable = false)
    private boolean payExtraAgreed;

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

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PmEnums.ChangeOrderType getChangeType() {
        return changeType;
    }

    public void setChangeType(PmEnums.ChangeOrderType changeType) {
        this.changeType = changeType;
    }

    public PmEnums.PmParty getRaisedBy() {
        return raisedBy;
    }

    public void setRaisedBy(PmEnums.PmParty raisedBy) {
        this.raisedBy = raisedBy;
    }

    public String getRaisedByName() {
        return raisedByName;
    }

    public void setRaisedByName(String raisedByName) {
        this.raisedByName = raisedByName;
    }

    public PmEnums.PmParty getApproverParty() {
        return approverParty;
    }

    public void setApproverParty(PmEnums.PmParty approverParty) {
        this.approverParty = approverParty;
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

    public String getDecisionRemarks() {
        return decisionRemarks;
    }

    public void setDecisionRemarks(String decisionRemarks) {
        this.decisionRemarks = decisionRemarks;
    }

    public java.math.BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(java.math.BigDecimal amount) {
        this.amount = amount;
    }

    public boolean isAutoRaised() {
        return autoRaised;
    }

    public void setAutoRaised(boolean autoRaised) {
        this.autoRaised = autoRaised;
    }

    public boolean isPayExtraAgreed() {
        return payExtraAgreed;
    }

    public void setPayExtraAgreed(boolean payExtraAgreed) {
        this.payExtraAgreed = payExtraAgreed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
