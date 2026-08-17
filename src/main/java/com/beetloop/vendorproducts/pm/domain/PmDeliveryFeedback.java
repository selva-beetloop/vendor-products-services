package com.beetloop.vendorproducts.pm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Buyer delivery feedback — BRD §6.11 (PM-13), ID series DFB.
 *
 * <p>Keyed to the shipment (§12.2). The buyer records how the delivered product
 * or formulation item performed; the vendor views this at project level.
 */
@Entity
@Table(name = "pm_delivery_feedback", indexes = {@Index(name = "idx_pm_delivery_feedback_code", columnList = "code"), @Index(name = "idx_pm_delivery_feedback_parent", columnList = "project_id")})
public class PmDeliveryFeedback {

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

    /** §12.2 — DFB is keyed to the Shipment ID. */
    @Column(name = "shipment_code", length = 40, nullable = false)
    private String shipmentCode;

    /** 1–5 overall rating. */
    @Column(name = "rating")
    private Integer rating;

    @Column(name = "quality_rating")
    private Integer qualityRating;

    @Column(name = "packaging_rating")
    private Integer packagingRating;

    @Column(name = "timeliness_rating")
    private Integer timelinessRating;

    @Column(name = "comments", length = 4000)
    private String comments;

    @Column(name = "accepted", nullable = false)
    private boolean accepted;

    @Column(name = "submitted_by_name", length = 200)
    private String submittedByName;

    @Column(name = "submitted_at")
    private Instant submittedAt;

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

    public String getShipmentCode() {
        return shipmentCode;
    }

    public void setShipmentCode(String shipmentCode) {
        this.shipmentCode = shipmentCode;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public Integer getQualityRating() {
        return qualityRating;
    }

    public void setQualityRating(Integer qualityRating) {
        this.qualityRating = qualityRating;
    }

    public Integer getPackagingRating() {
        return packagingRating;
    }

    public void setPackagingRating(Integer packagingRating) {
        this.packagingRating = packagingRating;
    }

    public Integer getTimelinessRating() {
        return timelinessRating;
    }

    public void setTimelinessRating(Integer timelinessRating) {
        this.timelinessRating = timelinessRating;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public String getSubmittedByName() {
        return submittedByName;
    }

    public void setSubmittedByName(String submittedByName) {
        this.submittedByName = submittedByName;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
