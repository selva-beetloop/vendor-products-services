package com.beetloop.vendorproducts.pm.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.util.UUID;

/**
 * A query raised against an order — BRD §6.7 (PM-09), ID series QRY.
 *
 * <p>Deliberately distinct from {@link PmIssue}: queries are the conversation
 * thread on an order (vendor ↔ buyer ↔ internal team), whereas issues are
 * tracked exceptions with an assignee and a severity.
 */
public class PmQuery {

    @Id
    private UUID id;

    /** Business ID from the BRD §12.2 matrix. */
    @Indexed(unique = true)
    private String code;

    @Transient
    private PmOrder order;

    private int position;

    private String subject;

    private String body;

    private PmEnums.PmParty raisedBy;

    private String raisedByName;

    /** Stage the query was raised against, when stage-scoped. */
    private String stageCode;

    private boolean answered;

    private String answer;

    private Instant answeredAt;

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
