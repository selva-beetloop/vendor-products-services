package com.beetloop.catalog.qc;

import com.beetloop.catalog.shared.model.QcStatus;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** Append-only, one row per transition. */
@Builder
@Document(collection = "status_history")
@CompoundIndex(name = "entity_at", def = "{'entityId': 1, 'at': -1}")
public record StatusHistory(
        @Id String id,
        String entityType,
        String entityId,
        QcStatus fromStatus,
        QcStatus toStatus,
        String actorId,
        String actorRole,
        String reason,
        String requestId,
        Instant at) {
}
