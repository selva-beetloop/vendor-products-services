package com.beetloop.catalog.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

/**
 * "Upload documents once to your lab library, then link them to each service."
 * So these are a first-class shared collection, not embedded objects.
 *
 * `status` is DERIVED from expiryDate. The running UI renders DOCUMENT STATUS: Complete for
 * accreditations that expired in 2023 - it never evaluates expiry at all.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "document_library")
@CompoundIndex(name = "vendor_kind", def = "{'vendorId': 1, 'kind': 1}")
@CompoundIndex(name = "vendor_expiry", def = "{'vendorId': 1, 'expiryDate': 1}")
public class LibraryDocument {

    @Id
    private String id;

    private String vendorId;

    /** ACCREDITATION | CERTIFICATION | SUPPORT_DOC */
    private String kind;

    private String code;
    private String name;
    private String issuingBody;
    private String referenceNo;
    private String scope;

    private LocalDate issueDate;
    private LocalDate expiryDate;

    /** LAB_WIDE | SERVICE_SPECIFIC */
    @Builder.Default
    private String applicability = "LAB_WIDE";

    private String documentId;

    /** DERIVED: VALID | EXPIRING_SOON | EXPIRED. A client-supplied value is rejected, not merged. */
    private String status;
    private Instant statusComputedAt;

    private Instant createdAt;
    private Instant updatedAt;
}
