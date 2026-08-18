package com.beetloop.vendorproducts.documentstore.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DocumentStoreDocumentDto(
        String id,
        String documentId,
        String fileName,
        String fileNameOriginal,
        String fileType,
        Long fileSizeBytes,
        String documentType,
        String moduleReference,
        Instant createdAt,
        Instant uploadedAt) {

    public String resolvedId() {
        if (id != null && !id.isBlank()) {
            return id;
        }
        return documentId;
    }

    public String resolvedFileName() {
        if (fileName != null && !fileName.isBlank()) {
            return fileName;
        }
        return fileNameOriginal;
    }
}
