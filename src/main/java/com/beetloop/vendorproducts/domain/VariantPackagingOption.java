package com.beetloop.vendorproducts.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import java.util.UUID;

/** One row of the "Packaging" repeatable list (Packaging &amp; Samples tab). */
public class VariantPackagingOption {

    @Id
    private UUID id;

    @Transient
    private ProductVariant variant;

    private int position;

    private String packagingType;

    private String size;

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
