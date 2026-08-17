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
 * A vendor project — BRD §6.1.
 *
 * <p>Created when an RFQ flow completes and the buyer pays (§5.1): the RFQ
 * converts into a Project and its Job Orders become {@link PmOrder}s carrying the
 * stages defined during the RFQ. {@link #rfqId} retains the originating RFQ ID as
 * required by the §12.2 ID matrix.
 */
@Entity
@Table(name = "pm_project", indexes = {
        @Index(name = "idx_pm_project_code", columnList = "project_code", unique = true),
        @Index(name = "idx_pm_project_vendor", columnList = "vendor_id"),
        @Index(name = "idx_pm_project_status", columnList = "status")
})
public class PmProject {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Business ID — PRJ-YYYY-NNNN (§12.1). */
    @Column(name = "project_code", length = 40, nullable = false, unique = true)
    private String projectCode;

    /** Originating RFQ, retained per the §12.2 matrix. */
    @Column(name = "rfq_id", length = 40)
    private String rfqId;

    @Column(name = "vendor_id", length = 120)
    private String vendorId;

    @Column(name = "name", length = 400, nullable = false)
    private String name;

    @Column(name = "buyer_name", length = 300)
    private String buyerName;

    @Column(name = "buyer_id", length = 120)
    private String buyerId;

    @Column(name = "project_manager", length = 200)
    private String projectManager;

    @Enumerated(EnumType.STRING)
    @Column(name = "track", length = 40)
    private PmEnums.ProjectTrack track;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private PmEnums.ProjectStatus status = PmEnums.ProjectStatus.ACTIVE;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "contract_value", precision = 19, scale = 2)
    private BigDecimal contractValue;

    @Column(name = "currency", length = 10)
    private String currency = "INR";

    @Column(name = "description", length = 4000)
    private String description;

    // ---- Active Projects card fields (§6.1) ----

    /** e.g. "Nutraceuticals", "Raw Material", "Machinery" — shown under the name. */
    @Column(name = "product_category", length = 200)
    private String productCategory;

    /** Risk badge + card accent on the project card. */
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 20)
    private PmEnums.RiskLevel riskLevel = PmEnums.RiskLevel.GREEN;

    @Column(name = "profit_margin")
    private Integer profitMargin;

    @Column(name = "vendor_pm_name", length = 200)
    private String vendorPmName;

    @Column(name = "vendor_pm_avatar", length = 10)
    private String vendorPmAvatar;

    @Column(name = "beetloop_am_name", length = 200)
    private String beetloopAmName;

    @Column(name = "beetloop_am_avatar", length = 10)
    private String beetloopAmAvatar;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private List<PmProjectLineItem> lineItems = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private List<PmOrder> orders = new ArrayList<>();

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

    public void addLineItem(PmProjectLineItem item) {
        item.setProject(this);
        item.setPosition(this.lineItems.size());
        this.lineItems.add(item);
    }

    public void addOrder(PmOrder order) {
        order.setProject(this);
        order.setPosition(this.orders.size());
        this.orders.add(order);
    }

    // ---- getters / setters ----

    public UUID getId() {
        return id;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public String getRfqId() {
        return rfqId;
    }

    public void setRfqId(String rfqId) {
        this.rfqId = rfqId;
    }

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(String buyerId) {
        this.buyerId = buyerId;
    }

    public String getProjectManager() {
        return projectManager;
    }

    public void setProjectManager(String projectManager) {
        this.projectManager = projectManager;
    }

    public PmEnums.ProjectTrack getTrack() {
        return track;
    }

    public void setTrack(PmEnums.ProjectTrack track) {
        this.track = track;
    }

    public PmEnums.ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(PmEnums.ProjectStatus status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public BigDecimal getContractValue() {
        return contractValue;
    }

    public void setContractValue(BigDecimal contractValue) {
        this.contractValue = contractValue;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(String productCategory) {
        this.productCategory = productCategory;
    }

    public PmEnums.RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(PmEnums.RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Integer getProfitMargin() {
        return profitMargin;
    }

    public void setProfitMargin(Integer profitMargin) {
        this.profitMargin = profitMargin;
    }

    public String getVendorPmName() {
        return vendorPmName;
    }

    public void setVendorPmName(String vendorPmName) {
        this.vendorPmName = vendorPmName;
    }

    public String getVendorPmAvatar() {
        return vendorPmAvatar;
    }

    public void setVendorPmAvatar(String vendorPmAvatar) {
        this.vendorPmAvatar = vendorPmAvatar;
    }

    public String getBeetloopAmName() {
        return beetloopAmName;
    }

    public void setBeetloopAmName(String beetloopAmName) {
        this.beetloopAmName = beetloopAmName;
    }

    public String getBeetloopAmAvatar() {
        return beetloopAmAvatar;
    }

    public void setBeetloopAmAvatar(String beetloopAmAvatar) {
        this.beetloopAmAvatar = beetloopAmAvatar;
    }

    /**
     * Overall completion across the project's orders — the "Completion %" bar on
     * the card. Averages order progress so a project with no orders reads 0.
     */
    public int completionPercent() {
        if (orders.isEmpty()) {
            return 0;
        }
        return (int) Math.round(orders.stream().mapToInt(PmOrder::completionPercent).average().orElse(0));
    }

    /** "2/4 complete" on the card. */
    public long completedOrderCount() {
        return orders.stream()
                .filter(o -> o.getStatus() == PmEnums.OrderStatus.COMPLETED
                        || o.getStatus() == PmEnums.OrderStatus.CLOSED)
                .count();
    }

    public List<PmProjectLineItem> getLineItems() {
        return lineItems;
    }

    public List<PmOrder> getOrders() {
        return orders;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
