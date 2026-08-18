package com.beetloop.vendorproducts.pm.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;

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
public class PmOrder {

    @Id
    private UUID id;

    /** Business ID — ORD-YYYY-NNNN. */
    @Indexed(unique = true)
    private String orderCode;

    @Transient
    private PmProject project;

    private int position;

    /** Originating job order from the RFQ, kept for traceability (§5.1). */
    private String jobOrderId;

    private String name;

    private String description;

    private PmEnums.OrderStatus status = PmEnums.OrderStatus.NOT_STARTED;

    private String owner;

    private LocalDate startDate;

    private LocalDate dueDate;

    private BigDecimal orderValue;

    /** SLA column on the All Orders list (On Track / At Risk / Delayed). */
    private PmEnums.SlaState slaState = PmEnums.SlaState.ON_TRACK;

    /** BUYER column on All Orders — denormalised from the project for the list. */
    private String buyerName;

    private List<PmStage> stages = new ArrayList<>();

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
