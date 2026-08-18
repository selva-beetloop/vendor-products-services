package com.beetloop.vendorproducts.pm.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;

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
public class PmProjectLineItem {

    @Id
    private UUID id;

    /** Business ID — PLI-YYYY-NNNN. */
    @Indexed(unique = true)
    private String lineCode;

    @Transient
    private PmProject project;

    private int position;

    private String materialCode;

    private String materialName;

    private String specification;

    /** PM-02a — Finished Goods or Raw Material, taken from the RFQ. */
    private PmEnums.ProductClass productClass = PmEnums.ProductClass.FINISHED_GOODS;

    private BigDecimal quantity;

    private String uom;

    /** RM-01 — result of the last stock check, held against the line. */
    private PmEnums.StockState stockState = PmEnums.StockState.NOT_CHECKED;

    private BigDecimal availableQuantity;

    /** Free text describing the chosen route, e.g. "FEFO issue" or "Internal BOM". */
    private String fulfilmentRoute;

    private String status;

    /** Linked BOM once Open BOM has been used on a finished-goods line. */
    private String bomId;

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
