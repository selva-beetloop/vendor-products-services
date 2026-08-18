package com.beetloop.vendorproducts.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One "Add Specification" block on the Technical Specifications sub-step —
 * e.g. "Assay &amp; Purity Specification", "Physical Specifications".
 */
public class VariantSpecificationGroup {

    @Id
    private UUID id;

    @Transient
    private ProductVariant variant;

    private int position;

    private String title;

    /** "Primary" or "Optional" — the tag chip shown on the section header. */
    private String tag;

    private boolean collapsed;

    private List<VariantSpecificationParameter> parameters = new ArrayList<>();

    public void replaceParameters(List<VariantSpecificationParameter> rows) {
        this.parameters.clear();
        int index = 0;
        for (VariantSpecificationParameter row : rows) {
            row.setGroup(this);
            row.setPosition(index++);
            this.parameters.add(row);
        }
    }

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    public List<VariantSpecificationParameter> getParameters() {
        return parameters;
    }
}
