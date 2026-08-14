package com.beetloop.vendorproducts.domain;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Scalar part of the "Commercial &amp; Pricing" sub-step, covering all four tabs:
 * Pricing &amp; Quantity, Commercial &amp; Trade Terms, Packaging &amp; Samples and
 * Included Documents &amp; Services. The repeatable lists live in
 * {@link VariantPriceTier} and {@link VariantPackagingOption}.
 */
@Embeddable
public class CommercialPricing {

    // ---- Tab 1: Pricing & Quantity → Base Pricing ----

    @Column(name = "cp_unit", length = 60)
    private String unit;

    @Column(name = "cp_base_cost", precision = 19, scale = 4)
    private BigDecimal baseCost;

    @Column(name = "cp_moq", length = 80)
    private String moq;

    @Column(name = "cp_lead_time", length = 120)
    private String leadTime;

    // ---- Tab 1: Commercial Charges ----

    @Column(name = "cp_freight_charges", length = 120)
    private String freightCharges;

    @Column(name = "cp_insurance_charges", length = 120)
    private String insuranceCharges;

    @Column(name = "cp_handling_charges", length = 120)
    private String handlingCharges;

    @Column(name = "cp_other_charges", length = 120)
    private String otherCharges;

    // ---- Tab 2: Commercial & Trade Terms ----

    @Column(name = "cp_currency", length = 40)
    private String currency;

    @Column(name = "cp_payment_terms", length = 200)
    private String paymentTerms;

    @Column(name = "cp_incoterms", length = 120)
    private String incoterms;

    @Column(name = "cp_price_validity_days", length = 60)
    private String priceValidityDays;

    @Column(name = "cp_gst_taxes", length = 80)
    private String gstTaxes;

    @Column(name = "cp_export_available")
    private Boolean exportAvailable;

    @Column(name = "cp_min_order_value", length = 80)
    private String minOrderValue;

    @Column(name = "cp_partial_shipment_allowed")
    private Boolean partialShipmentAllowed;

    @Column(name = "cp_return_policy", length = 2000)
    private String returnPolicy;

    @Column(name = "cp_warranty_period", length = 120)
    private String warrantyPeriod;

    // ---- Tab 3: Packaging & Samples → Sample Information ----

    @Column(name = "cp_sample_available")
    private Boolean sampleAvailable;

    @Column(name = "cp_free_paid_sample", length = 40)
    private String freePaidSample;

    @Column(name = "cp_sample_cost", length = 80)
    private String sampleCost;

    @Column(name = "cp_sample_turnaround_days", length = 80)
    private String sampleTurnaroundDays;

    @Column(name = "cp_max_sample_qty", length = 80)
    private String maxSampleQty;

    @Column(name = "cp_sample_shipping_borne_by", length = 40)
    private String sampleShippingBorneBy;

    // ---- Tab 4: Included Documents & Services (checkbox groups) ----

    @Type(JsonType.class)
    @Column(name = "cp_included_documents", columnDefinition = "text")
    private List<String> includedDocuments = new ArrayList<>();

    @Type(JsonType.class)
    @Column(name = "cp_included_services", columnDefinition = "text")
    private List<String> includedServices = new ArrayList<>();

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getBaseCost() {
        return baseCost;
    }

    public void setBaseCost(BigDecimal baseCost) {
        this.baseCost = baseCost;
    }

    public String getMoq() {
        return moq;
    }

    public void setMoq(String moq) {
        this.moq = moq;
    }

    public String getLeadTime() {
        return leadTime;
    }

    public void setLeadTime(String leadTime) {
        this.leadTime = leadTime;
    }

    public String getFreightCharges() {
        return freightCharges;
    }

    public void setFreightCharges(String freightCharges) {
        this.freightCharges = freightCharges;
    }

    public String getInsuranceCharges() {
        return insuranceCharges;
    }

    public void setInsuranceCharges(String insuranceCharges) {
        this.insuranceCharges = insuranceCharges;
    }

    public String getHandlingCharges() {
        return handlingCharges;
    }

    public void setHandlingCharges(String handlingCharges) {
        this.handlingCharges = handlingCharges;
    }

    public String getOtherCharges() {
        return otherCharges;
    }

    public void setOtherCharges(String otherCharges) {
        this.otherCharges = otherCharges;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public void setPaymentTerms(String paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    public String getIncoterms() {
        return incoterms;
    }

    public void setIncoterms(String incoterms) {
        this.incoterms = incoterms;
    }

    public String getPriceValidityDays() {
        return priceValidityDays;
    }

    public void setPriceValidityDays(String priceValidityDays) {
        this.priceValidityDays = priceValidityDays;
    }

    public String getGstTaxes() {
        return gstTaxes;
    }

    public void setGstTaxes(String gstTaxes) {
        this.gstTaxes = gstTaxes;
    }

    public Boolean getExportAvailable() {
        return exportAvailable;
    }

    public void setExportAvailable(Boolean exportAvailable) {
        this.exportAvailable = exportAvailable;
    }

    public String getMinOrderValue() {
        return minOrderValue;
    }

    public void setMinOrderValue(String minOrderValue) {
        this.minOrderValue = minOrderValue;
    }

    public Boolean getPartialShipmentAllowed() {
        return partialShipmentAllowed;
    }

    public void setPartialShipmentAllowed(Boolean partialShipmentAllowed) {
        this.partialShipmentAllowed = partialShipmentAllowed;
    }

    public String getReturnPolicy() {
        return returnPolicy;
    }

    public void setReturnPolicy(String returnPolicy) {
        this.returnPolicy = returnPolicy;
    }

    public String getWarrantyPeriod() {
        return warrantyPeriod;
    }

    public void setWarrantyPeriod(String warrantyPeriod) {
        this.warrantyPeriod = warrantyPeriod;
    }

    public Boolean getSampleAvailable() {
        return sampleAvailable;
    }

    public void setSampleAvailable(Boolean sampleAvailable) {
        this.sampleAvailable = sampleAvailable;
    }

    public String getFreePaidSample() {
        return freePaidSample;
    }

    public void setFreePaidSample(String freePaidSample) {
        this.freePaidSample = freePaidSample;
    }

    public String getSampleCost() {
        return sampleCost;
    }

    public void setSampleCost(String sampleCost) {
        this.sampleCost = sampleCost;
    }

    public String getSampleTurnaroundDays() {
        return sampleTurnaroundDays;
    }

    public void setSampleTurnaroundDays(String sampleTurnaroundDays) {
        this.sampleTurnaroundDays = sampleTurnaroundDays;
    }

    public String getMaxSampleQty() {
        return maxSampleQty;
    }

    public void setMaxSampleQty(String maxSampleQty) {
        this.maxSampleQty = maxSampleQty;
    }

    public String getSampleShippingBorneBy() {
        return sampleShippingBorneBy;
    }

    public void setSampleShippingBorneBy(String sampleShippingBorneBy) {
        this.sampleShippingBorneBy = sampleShippingBorneBy;
    }

    public List<String> getIncludedDocuments() {
        return includedDocuments;
    }

    public void setIncludedDocuments(List<String> includedDocuments) {
        this.includedDocuments = includedDocuments;
    }

    public List<String> getIncludedServices() {
        return includedServices;
    }

    public void setIncludedServices(List<String> includedServices) {
        this.includedServices = includedServices;
    }
}
