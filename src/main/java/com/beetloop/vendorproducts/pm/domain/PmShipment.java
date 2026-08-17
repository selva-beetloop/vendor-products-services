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
 * A shipment — BRD §6.9 / BP-25 / BP-26, ID series SHP.
 *
 * <p>One shared SHP series spans batch and formulation shipments (§12.3). A batch
 * shipment carries BOM, Batch and Invoice IDs; a formulation shipment carries
 * Trial (+version), Base Prototype, BOM (+version) and Invoice IDs — both plus
 * quantity/units and packaging. Status flow (BP-25): Created → Dispatched →
 * In Transit → Delivered → QC.
 */
@Entity
@Table(name = "pm_shipment", indexes = {@Index(name = "idx_pm_shipment_code", columnList = "code"), @Index(name = "idx_pm_shipment_parent", columnList = "project_id")})
public class PmShipment {

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

    /** Batch | Formulation Trial | Raw Material. */
    @Column(name = "origin_type", length = 30)
    private String originType;

    @Column(name = "batch_code", length = 40)
    private String batchCode;

    @Column(name = "trial_code", length = 40)
    private String trialCode;

    @Column(name = "prototype_code", length = 40)
    private String prototypeCode;

    @Column(name = "bom_code", length = 40)
    private String bomCode;

    @Column(name = "invoice_code", length = 40)
    private String invoiceCode;

    @Column(name = "order_code", length = 40)
    private String orderCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private PmEnums.ShipmentStatus status;

    @Column(name = "quantity", precision = 19, scale = 3)
    private java.math.BigDecimal quantity;

    @Column(name = "uom", length = 30)
    private String uom;

    @Column(name = "packaging", length = 400)
    private String packaging;

    @Column(name = "carrier", length = 200)
    private String carrier;

    @Column(name = "tracking_number", length = 120)
    private String trackingNumber;

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "destination", length = 1000)
    private String destination;

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

    public String getOriginType() {
        return originType;
    }

    public void setOriginType(String originType) {
        this.originType = originType;
    }

    public String getBatchCode() {
        return batchCode;
    }

    public void setBatchCode(String batchCode) {
        this.batchCode = batchCode;
    }

    public String getTrialCode() {
        return trialCode;
    }

    public void setTrialCode(String trialCode) {
        this.trialCode = trialCode;
    }

    public String getPrototypeCode() {
        return prototypeCode;
    }

    public void setPrototypeCode(String prototypeCode) {
        this.prototypeCode = prototypeCode;
    }

    public String getBomCode() {
        return bomCode;
    }

    public void setBomCode(String bomCode) {
        this.bomCode = bomCode;
    }

    public String getInvoiceCode() {
        return invoiceCode;
    }

    public void setInvoiceCode(String invoiceCode) {
        this.invoiceCode = invoiceCode;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public PmEnums.ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(PmEnums.ShipmentStatus status) {
        this.status = status;
    }

    public java.math.BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(java.math.BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getUom() {
        return uom;
    }

    public void setUom(String uom) {
        this.uom = uom;
    }

    public String getPackaging() {
        return packaging;
    }

    public void setPackaging(String packaging) {
        this.packaging = packaging;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public Instant getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(Instant dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
