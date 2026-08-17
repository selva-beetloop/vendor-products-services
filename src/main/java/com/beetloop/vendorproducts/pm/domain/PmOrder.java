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
 * An order under a project — BRD §6.3 (PM-03), previously called a Job Order.
 *
 * <p>Orders arrive from the RFQ conversion (§5.1) and carry the stages defined
 * during the RFQ, e.g. order "Scientific Research" containing the stage
 * "Research documentation &amp; submit".
 */
@Entity
@Table(name = "pm_order", indexes = {
        @Index(name = "idx_pm_order_code", columnList = "order_code", unique = true),
        @Index(name = "idx_pm_order_project", columnList = "project_id")
})
public class PmOrder {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Business ID — ORD-YYYY-NNNN. */
    @Column(name = "order_code", length = 40, nullable = false, unique = true)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private PmProject project;

    @Column(name = "position", nullable = false)
    private int position;

    /** Originating job order from the RFQ, kept for traceability (§5.1). */
    @Column(name = "job_order_id", length = 40)
    private String jobOrderId;

    @Column(name = "name", length = 400, nullable = false)
    private String name;

    @Column(name = "description", length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private PmEnums.OrderStatus status = PmEnums.OrderStatus.NOT_STARTED;

    @Column(name = "owner", length = 200)
    private String owner;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "order_value", precision = 19, scale = 2)
    private BigDecimal orderValue;

    /** SLA column on the All Orders list (On Track / At Risk / Delayed). */
    @Enumerated(EnumType.STRING)
    @Column(name = "sla_state", length = 20)
    private PmEnums.SlaState slaState = PmEnums.SlaState.ON_TRACK;

    /** BUYER column on All Orders — denormalised from the project for the list. */
    @Column(name = "buyer_name", length = 300)
    private String buyerName;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private List<PmStage> stages = new ArrayList<>();

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

    public void addStage(PmStage stage) {
        stage.setOrder(this);
        stage.setPosition(this.stages.size());
        this.stages.add(stage);
    }

    /** Progress across the order's stages, used by the project overview (PM-02). */
    public int completionPercent() {
        if (stages.isEmpty()) {
            return 0;
        }
        long done = stages.stream().filter(PmStage::isFinished).count();
        return (int) Math.round((done * 100.0) / stages.size());
    }

    // ---- getters / setters ----

    public UUID getId() {
        return id;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
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

    public String getJobOrderId() {
        return jobOrderId;
    }

    public void setJobOrderId(String jobOrderId) {
        this.jobOrderId = jobOrderId;
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

    public PmEnums.OrderStatus getStatus() {
        return status;
    }

    public void setStatus(PmEnums.OrderStatus status) {
        this.status = status;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getOrderValue() {
        return orderValue;
    }

    public void setOrderValue(BigDecimal orderValue) {
        this.orderValue = orderValue;
    }

    public PmEnums.SlaState getSlaState() {
        return slaState;
    }

    public void setSlaState(PmEnums.SlaState slaState) {
        this.slaState = slaState;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public List<PmStage> getStages() {
        return stages;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
