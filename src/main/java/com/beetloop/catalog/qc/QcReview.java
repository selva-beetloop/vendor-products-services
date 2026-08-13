package com.beetloop.catalog.qc;

import com.beetloop.catalog.shared.model.QcStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "qc_reviews")
@CompoundIndex(name = "queue", def = "{'status': 1, 'submittedAt': 1}")
@CompoundIndex(name = "entity_revision", def = "{'entityId': 1, 'revision': -1}")
@CompoundIndex(name = "claim", def = "{'claimedBy': 1, 'claimExpiresAt': 1}")
public class QcReview {

    @Id
    private String id;

    /** PRODUCT_LISTING | SERVICE_LISTING */
    private String entityType;
    private String entityId;
    private String entityCode;
    private String vendorId;
    private String categoryCode;
    private String listingName;

    private QcStatus status;
    @Builder.Default
    private String priority = "NORMAL";

    private Instant submittedAt;
    private String submittedBy;
    private long submissionVersion;

    private String claimedBy;
    private Instant claimedAt;
    private Instant claimExpiresAt;

    private String decidedBy;
    private Instant decidedAt;
    private String decisionReason;

    /** Required on REJECTED, so the vendor knows what to fix. */
    @Builder.Default
    private List<FieldFeedback> fieldFeedback = new ArrayList<>();

    @Builder.Default
    private int revision = 1;

    private Instant createdAt;
    private Instant updatedAt;

    public record FieldFeedback(String step, String path, String message) {
    }
}
