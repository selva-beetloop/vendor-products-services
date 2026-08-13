package com.beetloop.catalog.shared.idempotency;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "idempotency_keys")
public class IdempotencyRecord {

    @Id
    private String id;

    private String vendorId;
    private String endpoint;
    private String requestDigest;
    private String responseJson;
    private int httpStatus;

    /** TTL index: keys are retained for beetloop.catalog.idempotency.ttl-hours. */
    @Indexed(expireAfterSeconds = 86_400)
    private Instant createdAt;
}
