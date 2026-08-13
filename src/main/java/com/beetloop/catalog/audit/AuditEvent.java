package com.beetloop.catalog.audit;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

/**
 * Append-only. Every write path produces one.
 *
 * The Packaging Materials attestation ("I am authorized to list and sell this packaging material on
 * Beetloop") is recorded here as ATTESTATION_RECORDED with actor and timestamp - not merely as a
 * boolean inside data{}. A boolean in a mutable document is not evidence; an append-only event is.
 */
@Builder
@Document(collection = "audit_events")
@CompoundIndex(name = "entity_at", def = "{'entityId': 1, 'at': -1}")
@CompoundIndex(name = "vendor_at", def = "{'vendorId': 1, 'at': -1}")
public record AuditEvent(
        @Id String id,
        String vendorId,
        String actorId,
        String action,
        String entityType,
        String entityId,
        Map<String, Object> detail,
        String requestId,
        Instant at) {

    public static final String PRODUCT_STEP_SAVED = "PRODUCT_STEP_SAVED";
    public static final String PRODUCT_OVERALL_SAVED = "PRODUCT_OVERALL_SAVED";
    public static final String PRODUCT_SUBMITTED = "PRODUCT_SUBMITTED";
    public static final String PRODUCT_WITHDRAWN = "PRODUCT_WITHDRAWN";
    public static final String SERVICE_STEP_SAVED = "SERVICE_STEP_SAVED";
    public static final String SERVICE_OVERALL_SAVED = "SERVICE_OVERALL_SAVED";
    public static final String SERVICE_SUBMITTED = "SERVICE_SUBMITTED";
    public static final String SERVICE_WITHDRAWN = "SERVICE_WITHDRAWN";
    public static final String VARIANT_SECTION_SAVED = "VARIANT_SECTION_SAVED";
    public static final String CONFIGURATION_SECTION_SAVED = "CONFIGURATION_SECTION_SAVED";
    public static final String ATTESTATION_RECORDED = "ATTESTATION_RECORDED";
    public static final String QC_APPROVED = "QC_APPROVED";
    public static final String QC_REJECTED = "QC_REJECTED";
    public static final String QC_CLAIMED = "QC_CLAIMED";
    public static final String DOCUMENT_UPLOADED = "DOCUMENT_UPLOADED";
}
