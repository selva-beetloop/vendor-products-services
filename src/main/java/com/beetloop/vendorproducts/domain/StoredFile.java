package com.beetloop.vendorproducts.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

/**
 * Metadata for a file uploaded through {@code POST /uploads}. The bytes live on
 * local disk under the configured storage root; this row is the stable reference
 * the frontend attaches to a spec parameter, compliance document or image field.
 */
@Document(collection = "stored_file")
public class StoredFile {

    @Id
    private UUID id;

    private String originalFilename;

    private String contentType;

    private long sizeBytes;

    /** Path relative to the storage root. */
    private String storagePath;

    /** Logical grouping, e.g. {@code compliance-document}, {@code variant-image}. */
    private String module;

    private String referenceId;

    private String uploadedBy;

    private Instant createdAt;

    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
