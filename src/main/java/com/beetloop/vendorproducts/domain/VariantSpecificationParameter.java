package com.beetloop.vendorproducts.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import java.util.UUID;

/**
 * One "Add Parameter" row inside a specification group. Mirrors
 * {@code SpecRowState} in {@code AddMaterialVariantPage.tsx} plus the extra
 * columns the machinery/packaging variants add ({@code priority},
 * {@code remarks}).
 */
public class VariantSpecificationParameter {

    @Id
    private UUID id;

    @Transient
    private VariantSpecificationGroup group;

    private int position;

    /** UI label: "Parameter". */
    private String parameter;

    /** UI label: "Specification" (the value/limit). */
    private String specification;

    private String unit;

    /** UI label: "Test Method / Standard". */
    private String method;

    /** UI label: "Requirement Source" — Buyer Specification / Regulatory / Internal / Calculated. */
    private String source;

    /** Stored-file reference produced by POST /uploads, or a plain filename. */
    private String attachment;

    private String priority;

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
