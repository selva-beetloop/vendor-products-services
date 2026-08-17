package com.beetloop.vendorproducts.pm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A dependency between two stages/orders — BRD §6.4 (PM-05), ID series DEP.
 *
 * <p>Both linked references are displayed so the vendor can see what blocks, and
 * is blocked by, each stage (§12.2: "Show predecessor &amp; successor").
 */
@Entity
@Table(name = "pm_dependency", indexes = {@Index(name = "idx_pm_dependency_code", columnList = "code"), @Index(name = "idx_pm_dependency_parent", columnList = "project_id")})
public class PmDependency {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Business ID from the BRD §12.2 matrix. */
    @Column(name = "code", length = 40, nullable = false, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private PmProject project;

    @Column(name = "position", nullable = false)
    private int position;

    /** Stage/order that must happen first. */
    @Column(name = "predecessor_code", length = 40, nullable = false)
    private String predecessorCode;

    @Column(name = "predecessor_name", length = 400)
    private String predecessorName;

    /** Stage/order that waits on the predecessor. */
    @Column(name = "successor_code", length = 40, nullable = false)
    private String successorCode;

    @Column(name = "successor_name", length = 400)
    private String successorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "dependency_type", length = 40, nullable = false)
    private PmEnums.DependencyType dependencyType;

    @Column(name = "lag_days")
    private Integer lagDays;

    /** True when the successor cannot start until this clears. */
    @Column(name = "blocking", nullable = false)
    private boolean blocking;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
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
