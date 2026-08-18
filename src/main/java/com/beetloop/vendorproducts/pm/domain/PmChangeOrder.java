package com.beetloop.vendorproducts.pm.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;

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
public class PmChangeOrder {

    @Id
    private UUID id;

    /** Business ID from the BRD §12.2 matrix. */
    @Indexed(unique = true)
    private String code;

    @Transient
    private PmProject project;

    private int position;

    /** Order the change applies to. */
    private String orderCode;

    private String title;

    private String description;

    private PmEnums.ChangeOrderType changeType;

    /** PM-07 — raiser visibility is required on both views. */
    private PmEnums.PmParty raisedBy;

    private String raisedByName;

    /** Counterparty who must approve. */
    private PmEnums.PmParty approverParty;

    private PmEnums.ApprovalDecision decision;

    private Instant decidedAt;

    private String decisionRemarks;

    /** Added to the stage payment request once approved (BP-23). */
    private java.math.BigDecimal amount;

    /** True for the system-raised Rework CO. */
    private boolean autoRaised;

    /** Buyer's "I will agree to pay extra for rework" checkbox. */
    private boolean payExtraAgreed;

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
