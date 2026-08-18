package com.beetloop.vendorproducts.pm.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.util.UUID;

/**
 * A task raised against a stage — BRD PM-04 ("Add Task").
 *
 * <p>All tasks must be done before the stage can be submitted for QC review.
 */
public class PmTask {

    @Id
    private UUID id;

    /** Business ID from the BRD §12.2 matrix. */
    @Indexed(unique = true)
    private String code;

    @Transient
    private PmStage stage;

    private int position;

    private String title;

    private String description;

    /** Open → In Progress → Done. */
    private PmEnums.TaskStatus status;

    private String assignee;

    private java.time.LocalDate dueDate;

    /** PM-04 — tasks may originate with the vendor, buyer or QC. */
    private PmEnums.PmParty raisedBy;

    /** PRIORITY column on the All Tasks list. */
    private PmEnums.TaskPriority priority = PmEnums.TaskPriority.MEDIUM;

    /** PROGRESS column on the All Tasks list (0–100). */
    private Integer progress;

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
