package com.beetloop.vendorproducts.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Scalar part of the "Commercial &amp; Pricing" sub-step, covering all four tabs:
 * Pricing &amp; Quantity, Commercial &amp; Trade Terms, Packaging &amp; Samples and
 * Included Documents &amp; Services. The repeatable lists live in
 * {@link VariantPriceTier} and {@link VariantPackagingOption}.
 */
public class CommercialPricing {

    // ---- Tab 1: Pricing & Quantity → Base Pricing ----

    private String unit;

    private BigDecimal baseCost;

    private String moq;

    private String leadTime;

    // ---- Tab 1: Commercial Charges ----

    private String freightCharges;

    private String insuranceCharges;

    private String handlingCharges;

    private String otherCharges;

    // ---- Tab 2: Commercial & Trade Terms ----

    private String currency;

    private String paymentTerms;

    private String incoterms;

    private String priceValidityDays;

    private String gstTaxes;

    private Boolean exportAvailable;

    private String minOrderValue;

    private Boolean partialShipmentAllowed;

    private String returnPolicy;

    private String warrantyPeriod;

    // ---- Tab 3: Packaging & Samples → Sample Information ----

    private Boolean sampleAvailable;

    private String freePaidSample;

    private String sampleCost;

    private String sampleTurnaroundDays;

    private String maxSampleQty;

    private String sampleShippingBorneBy;

    // ---- Tab 4: Included Documents & Services (checkbox groups) ----

    private List<String> includedDocuments = new ArrayList<>();

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
