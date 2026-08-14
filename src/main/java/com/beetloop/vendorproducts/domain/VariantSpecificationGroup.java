package com.beetloop.vendorproducts.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One "Add Specification" block on the Technical Specifications sub-step —
 * e.g. "Assay &amp; Purity Specification", "Physical Specifications".
 */
@Entity
@Table(name = "variant_specification_group", indexes = {
        @Index(name = "idx_spec_group_variant", columnList = "variant_id")
})
public class VariantSpecificationGroup {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "title", length = 400)
    private String title;

    /** "Primary" or "Optional" — the tag chip shown on the section header. */
    @Column(name = "tag", length = 40)
    private String tag;

    @Column(name = "collapsed")
    private boolean collapsed;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("position ASC")
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
