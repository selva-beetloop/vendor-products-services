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
 * A query raised against an order — BRD §6.7 (PM-09), ID series QRY.
 *
 * <p>Deliberately distinct from {@link PmIssue}: queries are the conversation
 * thread on an order (vendor ↔ buyer ↔ internal team), whereas issues are
 * tracked exceptions with an assignee and a severity.
 */
@Entity
@Table(name = "pm_query", indexes = {@Index(name = "idx_pm_query_code", columnList = "code"), @Index(name = "idx_pm_query_parent", columnList = "order_id")})
public class PmQuery {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Business ID from the BRD §12.2 matrix. */
    @Column(name = "code", length = 40, nullable = false, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private PmOrder order;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "subject", length = 400, nullable = false)
    private String subject;

    @Column(name = "body", length = 4000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "raised_by", length = 30, nullable = false)
    private PmEnums.PmParty raisedBy;

    @Column(name = "raised_by_name", length = 200)
    private String raisedByName;

    /** Stage the query was raised against, when stage-scoped. */
    @Column(name = "stage_code", length = 40)
    private String stageCode;

    @Column(name = "answered", nullable = false)
    private boolean answered;

    @Column(name = "answer", length = 4000)
    private String answer;

    @Column(name = "answered_at")
    private Instant answeredAt;

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

    public PmOrder getOrder() {
        return order;
    }

    public void setOrder(PmOrder order) {
        this.order = order;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public PmEnums.PmParty getRaisedBy() {
        return raisedBy;
    }

    public void setRaisedBy(PmEnums.PmParty raisedBy) {
        this.raisedBy = raisedBy;
    }

    public String getRaisedByName() {
        return raisedByName;
    }

    public void setRaisedByName(String raisedByName) {
        this.raisedByName = raisedByName;
    }

    public String getStageCode() {
        return stageCode;
    }

    public void setStageCode(String stageCode) {
        this.stageCode = stageCode;
    }

    public boolean isAnswered() {
        return answered;
    }

    public void setAnswered(boolean answered) {
        this.answered = answered;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(Instant answeredAt) {
        this.answeredAt = answeredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
