package com.beetloop.vendorproducts.pm.domain;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A stage within an order — the unit the BRD §5.2 delivery cycle turns on.
 *
 * <pre>
 * Execute (tasks + checklists)
 *   → submit for QC review        (PM-04)
 *   → Orders &amp; Stages QC approves or rejects with queries
 *   → buyer approval button enabled, buyer approves or requests rework
 *   → Request for Payment appears, routed to Beetloop Finance
 * </pre>
 *
 * <p>{@link #paymentEligible} reflects §5.2 step 4: "the stage builder determines
 * which stages are payment-eligible", so not every stage can raise a payment
 * request even once approved.
 */
@Entity
@Table(name = "pm_stage", indexes = {
        @Index(name = "idx_pm_stage_code", columnList = "stage_code", unique = true),
        @Index(name = "idx_pm_stage_order", columnList = "order_id")
})
public class PmStage {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Business ID — STG-YYYY-NNNN. The positional label (STG-01, STG-02…) used in
     * the UI is derived from {@link #position}; see {@link #stageNumber()}.
     */
    @Column(name = "stage_code", length = 40, nullable = false, unique = true)
    private String stageCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private PmOrder order;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "name", length = 400, nullable = false)
    private String name;

    @Column(name = "description", length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 40, nullable = false)
    private PmEnums.StageStatus status = PmEnums.StageStatus.NOT_STARTED;

    /** §5.2 step 4 — only payment-eligible stages expose Request for Payment. */
    @Column(name = "payment_eligible", nullable = false)
    private boolean paymentEligible;

    @Column(name = "payment_amount", precision = 19, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "owner", length = 200)
    private String owner;

    @Column(name = "qc_submitted_at")
    private Instant qcSubmittedAt;

    @Column(name = "qc_decided_at")
    private Instant qcDecidedAt;

    @Column(name = "qc_remarks", length = 2000)
    private String qcRemarks;

    @Column(name = "buyer_decided_at")
    private Instant buyerDecidedAt;

    @Column(name = "buyer_remarks", length = 2000)
    private String buyerRemarks;

    @Column(name = "payment_requested_at")
    private Instant paymentRequestedAt;

    @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private List<PmTask> tasks = new ArrayList<>();

    @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private List<PmChecklistItem> checklist = new ArrayList<>();

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

    public void addTask(PmTask task) {
        task.setStage(this);
        task.setPosition(this.tasks.size());
        this.tasks.add(task);
    }

    public void addChecklistItem(PmChecklistItem item) {
        item.setStage(this);
        item.setPosition(this.checklist.size());
        this.checklist.add(item);
    }

    /** Positional label shown in the UI and referenced by QC and the buyer (§12.2). */
    public String stageNumber() {
        return String.format("STG-%02d", position + 1);
    }

    public boolean isFinished() {
        return status == PmEnums.StageStatus.COMPLETED
                || status == PmEnums.StageStatus.PAID
                || status == PmEnums.StageStatus.BUYER_APPROVED
                || status == PmEnums.StageStatus.PAYMENT_REQUESTED;
    }

    /**
     * PM-04 — a stage may only be submitted for QC once every task and checklist
     * item is done ("mark all as done; and submit for QC review").
     */
    public boolean allItemsDone() {
        boolean tasksDone = tasks.stream().allMatch(t -> t.getStatus() == PmEnums.TaskStatus.DONE
                || t.getStatus() == PmEnums.TaskStatus.CANCELLED);
        boolean checklistDone = checklist.stream().allMatch(PmChecklistItem::isDone);
        return tasksDone && checklistDone;
    }

    // ---- getters / setters ----

    public UUID getId() {
        return id;
    }

    public String getStageCode() {
        return stageCode;
    }

    public void setStageCode(String stageCode) {
        this.stageCode = stageCode;
    }

    public PmOrder getOrder() {
        return order;
    }

    public void setOrder(PmOrder order) {
        this.order = order;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PmEnums.StageStatus getStatus() {
        return status;
    }

    public void setStatus(PmEnums.StageStatus status) {
        this.status = status;
    }

    public boolean isPaymentEligible() {
        return paymentEligible;
    }

    public void setPaymentEligible(boolean paymentEligible) {
        this.paymentEligible = paymentEligible;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(BigDecimal paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Instant getQcSubmittedAt() {
        return qcSubmittedAt;
    }

    public void setQcSubmittedAt(Instant qcSubmittedAt) {
        this.qcSubmittedAt = qcSubmittedAt;
    }

    public Instant getQcDecidedAt() {
        return qcDecidedAt;
    }

    public void setQcDecidedAt(Instant qcDecidedAt) {
        this.qcDecidedAt = qcDecidedAt;
    }

    public String getQcRemarks() {
        return qcRemarks;
    }

    public void setQcRemarks(String qcRemarks) {
        this.qcRemarks = qcRemarks;
    }

    public Instant getBuyerDecidedAt() {
        return buyerDecidedAt;
    }

    public void setBuyerDecidedAt(Instant buyerDecidedAt) {
        this.buyerDecidedAt = buyerDecidedAt;
    }

    public String getBuyerRemarks() {
        return buyerRemarks;
    }

    public void setBuyerRemarks(String buyerRemarks) {
        this.buyerRemarks = buyerRemarks;
    }

    public Instant getPaymentRequestedAt() {
        return paymentRequestedAt;
    }

    public void setPaymentRequestedAt(Instant paymentRequestedAt) {
        this.paymentRequestedAt = paymentRequestedAt;
    }

    public List<PmTask> getTasks() {
        return tasks;
    }

    public List<PmChecklistItem> getChecklist() {
        return checklist;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
