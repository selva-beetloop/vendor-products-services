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

/**
 * One "Add Parameter" row inside a specification group. Mirrors
 * {@code SpecRowState} in {@code AddMaterialVariantPage.tsx} plus the extra
 * columns the machinery/packaging variants add ({@code priority},
 * {@code remarks}).
 */
@Entity
@Table(name = "variant_specification_parameter", indexes = {
        @Index(name = "idx_spec_param_group", columnList = "group_id")
})
public class VariantSpecificationParameter {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private VariantSpecificationGroup group;

    @Column(name = "position", nullable = false)
    private int position;

    /** UI label: "Parameter". */
    @Column(name = "parameter", length = 400)
    private String parameter;

    /** UI label: "Specification" (the value/limit). */
    @Column(name = "specification", length = 400)
    private String specification;

    @Column(name = "unit", length = 80)
    private String unit;

    /** UI label: "Test Method / Standard". */
    @Column(name = "method", length = 300)
    private String method;

    /** UI label: "Requirement Source" — Buyer Specification / Regulatory / Internal / Calculated. */
    @Column(name = "source", length = 120)
    private String source;

    /** Stored-file reference produced by POST /uploads, or a plain filename. */
    @Column(name = "attachment", length = 1000)
    private String attachment;

    @Column(name = "priority", length = 40)
    private String priority;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public VariantSpecificationGroup getGroup() {
        return group;
    }

    public void setGroup(VariantSpecificationGroup group) {
        this.group = group;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
