package com.beetloop.catalog.document;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class DocumentDtos {

    private DocumentDtos() {
    }

    public record UploadResponse(
            String documentId,
            String fileName,
            String mimeType,
            long sizeBytes,
            String displaySize,
            String displayType,
            String scanStatus,
            String checksumSha256,
            String url,
            Instant uploadedAt) {
    }

    /** issueDate / expiryDate accept free text; the response echoes the normalised ISO value. */
    public record LibraryDocumentRequest(
            @NotBlank String kind,
            String code,
            @NotBlank String name,
            String issuingBody,
            String referenceNo,
            String scope,
            Object issueDate,
            Object expiryDate,
            String applicability,
            String documentId,
            /** Accepted for round-tripping and then rejected - status is derived. */
            String status) {
    }

    public record LibraryDocumentResponse(
            String libraryDocumentId,
            String kind,
            String code,
            String name,
            String issuingBody,
            String referenceNo,
            String scope,
            LocalDate issueDate,
            LocalDate expiryDate,
            String applicability,
            String documentId,
            String status,
            Long daysExpired,
            Instant statusComputedAt,
            Map<String, String> dateInterpretation) {
    }

    public record DocumentLinkRequest(String selectionId, @NotBlank String libraryDocumentId,
                                      @NotBlank String linkType) {
    }

    public record DocumentLinkResponse(String linkId, String listingId, String selectionId,
                                       String libraryDocumentId, String linkType, String applicability) {
    }

    public record LibraryCounts(long valid, long expiringSoon, long expired) {
    }

    public record ReferencedBy(List<Map<String, Object>> referencedBy) {
    }
}
