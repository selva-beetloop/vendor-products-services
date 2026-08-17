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
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A project line item — the end product the project must deliver (BRD §6.2a).
 *
 * <p>The line is sourced from the RFQ, and PM-02b makes the offered action a
 * function of the RFQ product type: a Finished Goods line shows <em>Open BOM</em>
 * (routing to batch production or formulation trials), a Raw Material line shows
 * <em>Check Stock</em> (starting the §8A fulfilment track). This is the point at
 * which the two tracks diverge, so the action is derived rather than stored —
 * see {@link #resolveAction()}.
 */
@Entity
@Table(name = "pm_project_line_item", indexes = {
        @Index(name = "idx_pm_pli_code", columnList = "line_code", unique = true),
        @Index(name = "idx_pm_pli_project", columnList = "project_id")
})
public class PmProjectLineItem {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Business ID — PLI-YYYY-NNNN. */
    @Column(name = "line_code", length = 40, nullable = false, unique = true)
    private String lineCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private PmProject project;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "material_code", length = 120)
    private String materialCode;

    @Column(name = "material_name", length = 400)
    private String materialName;

    @Column(name = "specification", length = 2000)
    private String specification;

    /** PM-02a — Finished Goods or Raw Material, taken from the RFQ. */
    @Enumerated(EnumType.STRING)
    @Column(name = "product_class", length = 40, nullable = false)
    private PmEnums.ProductClass productClass = PmEnums.ProductClass.FINISHED_GOODS;

    @Column(name = "quantity", precision = 19, scale = 3)
    private BigDecimal quantity;

    @Column(name = "uom", length = 30)
    private String uom;

    /** RM-01 — result of the last stock check, held against the line. */
    @Enumerated(EnumType.STRING)
    @Column(name = "stock_state", length = 30, nullable = false)
    private PmEnums.StockState stockState = PmEnums.StockState.NOT_CHECKED;

    @Column(name = "available_quantity", precision = 19, scale = 3)
    private BigDecimal availableQuantity;

    /** Free text describing the chosen route, e.g. "FEFO issue" or "Internal BOM". */
    @Column(name = "fulfilment_route", length = 200)
    private String fulfilmentRoute;

    @Column(name = "status", length = 60)
    private String status;

    /** Linked BOM once Open BOM has been used on a finished-goods line. */
    @Column(name = "bom_id", length = 40)
    private String bomId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @jakarta.persistence.PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @jakarta.persistence.PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * PM-02b — the action button offered against this line.
     *
     * <p>Derived from the product class so it can never drift from it: Finished
     * Goods → "Open BOM"; Raw Material → "Check Stock", which becomes
     * "Issue via FEFO" or "Raise Internal BOM" once a stock check has run
     * (RM-02).
     */
    public String resolveAction() {
        if (productClass == PmEnums.ProductClass.FINISHED_GOODS) {
            return "Open BOM";
        }
        return switch (stockState) {
            case IN_STOCK -> "Issue via FEFO";
            case PARTIAL, NOT_AVAILABLE -> "Raise Internal BOM";
            case NOT_CHECKED -> "Check Stock";
        };
    }

    // ---- getters / setters ----

    public UUID getId() {
        return id;
    }

    public String getLineCode() {
        return lineCode;
    }

    public void setLineCode(String lineCode) {
        this.lineCode = lineCode;
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

    public String getMaterialCode() {
        return materialCode;
    }

    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public PmEnums.ProductClass getProductClass() {
        return productClass;
    }

    public void setProductClass(PmEnums.ProductClass productClass) {
        this.productClass = productClass;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getUom() {
        return uom;
    }

    public void setUom(String uom) {
        this.uom = uom;
    }

    public PmEnums.StockState getStockState() {
        return stockState;
    }

    public void setStockState(PmEnums.StockState stockState) {
        this.stockState = stockState;
    }

    public BigDecimal getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(BigDecimal availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public String getFulfilmentRoute() {
        return fulfilmentRoute;
    }

    public void setFulfilmentRoute(String fulfilmentRoute) {
        this.fulfilmentRoute = fulfilmentRoute;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBomId() {
        return bomId;
    }

    public void setBomId(String bomId) {
        this.bomId = bomId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
