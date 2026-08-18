package com.beetloop.vendorproducts.pm.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.util.UUID;

/**
 * A dependency between two stages/orders — BRD §6.4 (PM-05), ID series DEP.
 *
 * <p>Both linked references are displayed so the vendor can see what blocks, and
 * is blocked by, each stage (§12.2: "Show predecessor &amp; successor").
 */
public class PmDependency {

    @Id
    private UUID id;

    /** Business ID from the BRD §12.2 matrix. */
    @Indexed(unique = true)
    private String code;

    @Transient
    private PmProject project;

    private int position;

    /** Stage/order that must happen first. */
    private String predecessorCode;

    private String predecessorName;

    /** Stage/order that waits on the predecessor. */
    private String successorCode;

    private String successorName;

    private PmEnums.DependencyType dependencyType;

    private Integer lagDays;

    /** True when the successor cannot start until this clears. */
    private boolean blocking;

    private String notes;

    private Instant createdAt;

    private Instant updatedAt;

    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public PmProject getProject() {
        return project;
    }

    public void setProject(PmProject project) {
        this.project = project;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getPredecessorCode() {
        return predecessorCode;
    }

    public void setPredecessorCode(String predecessorCode) {
        this.predecessorCode = predecessorCode;
    }

    public String getPredecessorName() {
        return predecessorName;
    }

    public void setPredecessorName(String predecessorName) {
        this.predecessorName = predecessorName;
    }

    public String getSuccessorCode() {
        return successorCode;
    }

    public void setSuccessorCode(String successorCode) {
        this.successorCode = successorCode;
    }

    public String getSuccessorName() {
        return successorName;
    }

    public void setSuccessorName(String successorName) {
        this.successorName = successorName;
    }

    public PmEnums.DependencyType getDependencyType() {
        return dependencyType;
    }

    public void setDependencyType(PmEnums.DependencyType dependencyType) {
        this.dependencyType = dependencyType;
    }

    public Integer getLagDays() {
        return lagDays;
    }

    public void setLagDays(Integer lagDays) {
        this.lagDays = lagDays;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
