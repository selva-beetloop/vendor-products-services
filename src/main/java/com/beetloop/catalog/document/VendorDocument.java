package com.beetloop.catalog.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * fileName / mimeType / sizeBytes are exactly the three values the Finished Goods file card renders
 * as "HydraFit_Brand_Ownership.pdf - PDF - 1.2 MB", so the upload response must return all three.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "vendor_documents")
@CompoundIndex(name = "vendor_created", def = "{'vendorId': 1, 'createdAt': -1}")
public class VendorDocument {

    @Id
    private String id;

    private String vendorId;

    /** Display name only - never used as the storage key. */
    private String fileName;
    private String mimeType;
    private long sizeBytes;

    @Indexed(unique = true)
    private String storageKey;

    private String checksumSha256;

    /** PENDING | CLEAN | INFECTED - a document is referencable only once CLEAN. */
    @Builder.Default
    private String scanStatus = "PENDING";

    private String usage;
    private String linkedEntityType;
    private String linkedEntityId;
    private String linkedPath;

    private Instant createdAt;
    private String createdBy;
    private Instant deletedAt;
}
