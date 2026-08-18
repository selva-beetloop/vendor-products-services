package com.beetloop.vendorproducts.pm.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;

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
public class PmShipment {

    @Id
    private UUID id;

    /** Business ID from the BRD §12.2 matrix. */
    @Indexed(unique = true)
    private String code;

    @Transient
    private PmProject project;

    private int position;

    /** Batch | Formulation Trial | Raw Material. */
    private String originType;

    private String batchCode;

    private String trialCode;

    private String prototypeCode;

    private String bomCode;

    private String invoiceCode;

    private String orderCode;

    private PmEnums.ShipmentStatus status;

    private java.math.BigDecimal quantity;

    private String uom;

    private String packaging;

    private String carrier;

    private String trackingNumber;

    private Instant dispatchedAt;

    private Instant deliveredAt;

    private String destination;

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
