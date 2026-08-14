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

/** One row of the "Packaging" repeatable list (Packaging &amp; Samples tab). */
@Entity
@Table(name = "variant_packaging_option", indexes = {
        @Index(name = "idx_packaging_option_variant", columnList = "variant_id")
})
public class VariantPackagingOption {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "packaging_type", length = 200)
    private String packagingType;

    @Column(name = "size", length = 200)
    private String size;

    @Column(name = "custom_packaging", length = 400)
    private String customPackaging;

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

    public String getPackagingType() {
        return packagingType;
    }

    public void setPackagingType(String packagingType) {
        this.packagingType = packagingType;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getCustomPackaging() {
        return customPackaging;
    }

    public void setCustomPackaging(String customPackaging) {
        this.customPackaging = customPackaging;
    }
}
