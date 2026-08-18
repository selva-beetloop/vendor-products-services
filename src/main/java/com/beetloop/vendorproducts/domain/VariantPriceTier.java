package com.beetloop.vendorproducts.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import java.util.UUID;

/** One row of the "Volume Pricing" repeatable table (Pricing &amp; Quantity tab). */
public class VariantPriceTier {

    @Id
    private UUID id;

    @Transient
    private ProductVariant variant;

    private int position;

    /** UI label: "Quantity Range" / "Qty Break". */
    private String qtyBreak;

    /** UI label: "Tier Name" — e.g. "Base Price". */
    private String tierLabel;

    private String pricePerUnit;

    private String discountVsBase;

    private String leadTimeDays;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ProductVariant getVariant() {
        return variant;
    }

    public void setVariant(ProductVariant variant) {
        this.variant = variant;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getQtyBreak() {
        return qtyBreak;
    }

    public void setQtyBreak(String qtyBreak) {
        this.qtyBreak = qtyBreak;
    }

    public String getTierLabel() {
        return tierLabel;
    }

    public void setTierLabel(String tierLabel) {
        this.tierLabel = tierLabel;
    }

    public String getPricePerUnit() {
        return pricePerUnit;
    }

    public void setPricePerUnit(String pricePerUnit) {
        this.pricePerUnit = pricePerUnit;
    }

    public String getDiscountVsBase() {
        return discountVsBase;
    }

    public void setDiscountVsBase(String discountVsBase) {
        this.discountVsBase = discountVsBase;
    }

    public String getLeadTimeDays() {
        return leadTimeDays;
    }

    public void setLeadTimeDays(String leadTimeDays) {
        this.leadTimeDays = leadTimeDays;
    }
}
