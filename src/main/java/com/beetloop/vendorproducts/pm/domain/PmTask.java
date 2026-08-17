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
 * A task raised against a stage — BRD PM-04 ("Add Task").
 *
 * <p>All tasks must be done before the stage can be submitted for QC review.
 */
@Entity
@Table(name = "pm_task", indexes = {@Index(name = "idx_pm_task_code", columnList = "code"), @Index(name = "idx_pm_task_parent", columnList = "stage_id")})
public class PmTask {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Business ID from the BRD §12.2 matrix. */
    @Column(name = "code", length = 40, nullable = false, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id", nullable = false)
    private PmStage stage;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "title", length = 400, nullable = false)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    /** Open → In Progress → Done. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private PmEnums.TaskStatus status;

    @Column(name = "assignee", length = 200)
    private String assignee;

    @Column(name = "due_date")
    private java.time.LocalDate dueDate;

    /** PM-04 — tasks may originate with the vendor, buyer or QC. */
    @Enumerated(EnumType.STRING)
    @Column(name = "raised_by", length = 30)
    private PmEnums.PmParty raisedBy;

    /** PRIORITY column on the All Tasks list. */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20)
    private PmEnums.TaskPriority priority = PmEnums.TaskPriority.MEDIUM;

    /** PROGRESS column on the All Tasks list (0–100). */
    @Column(name = "progress")
    private Integer progress;

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

    public PmStage getStage() {
        return stage;
    }

    public void setStage(PmStage stage) {
        this.stage = stage;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PmEnums.TaskStatus getStatus() {
        return status;
    }

    public void setStatus(PmEnums.TaskStatus status) {
        this.status = status;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public java.time.LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(java.time.LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public PmEnums.PmParty getRaisedBy() {
        return raisedBy;
    }

    public void setRaisedBy(PmEnums.PmParty raisedBy) {
        this.raisedBy = raisedBy;
    }

    public PmEnums.TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(PmEnums.TaskPriority priority) {
        this.priority = priority;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
