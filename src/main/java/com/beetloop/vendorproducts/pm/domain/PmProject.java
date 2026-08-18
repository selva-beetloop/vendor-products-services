package com.beetloop.vendorproducts.pm.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

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
@Document(collection = "pm_project")
public class PmProject {

    @Id
    private UUID id;

    /** Business ID — PRJ-YYYY-NNNN (§12.1). */
    @Indexed(unique = true)
    private String projectCode;

    /** Originating RFQ, retained per the §12.2 matrix. */
    private String rfqId;

    private String vendorId;

    private String name;

    private String buyerName;

    private String buyerId;

    private String projectManager;

    private PmEnums.ProjectTrack track;

    private PmEnums.ProjectStatus status = PmEnums.ProjectStatus.ACTIVE;

    private LocalDate startDate;

    private LocalDate targetDate;

    private BigDecimal contractValue;

    private String currency = "INR";

    private String description;

    // ---- Active Projects card fields (§6.1) ----

    /** e.g. "Nutraceuticals", "Raw Material", "Machinery" — shown under the name. */
    private String productCategory;

    /** Risk badge + card accent on the project card. */
    private PmEnums.RiskLevel riskLevel = PmEnums.RiskLevel.GREEN;

    private Integer profitMargin;

    private String vendorPmName;

    private String vendorPmAvatar;

    private String beetloopAmName;

    private String beetloopAmAvatar;

    private List<PmProjectLineItem> lineItems = new ArrayList<>();

    private List<PmOrder> orders = new ArrayList<>();

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
