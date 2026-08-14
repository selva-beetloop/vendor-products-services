package com.beetloop.vendorproducts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

/** One row of the "Volume Pricing" repeatable table (Pricing &amp; Quantity tab). */
@Entity
@Table(name = "variant_price_tier", indexes = {
        @Index(name = "idx_price_tier_variant", columnList = "variant_id")
})
public class VariantPriceTier {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "position", nullable = false)
    private int position;

    /** UI label: "Quantity Range" / "Qty Break". */
    @Column(name = "qty_break", length = 200)
    private String qtyBreak;

    /** UI label: "Tier Name" — e.g. "Base Price". */
    @Column(name = "tier_label", length = 200)
    private String tierLabel;

    @Column(name = "price_per_unit", length = 80)
    private String pricePerUnit;

    @Column(name = "discount_vs_base", length = 80)
    private String discountVsBase;

    @Column(name = "lead_time_days", length = 80)
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
