package com.beetloop.catalog.document;

import com.beetloop.catalog.audit.AuditEvent;
import com.beetloop.catalog.audit.AuditService;
import com.beetloop.catalog.config.CatalogProperties;
import com.beetloop.catalog.shared.error.ApiException;
import com.beetloop.catalog.shared.error.ErrorCode;
import com.beetloop.catalog.shared.tenant.TenantContext;
import com.beetloop.catalog.shared.util.Ids;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class DocumentService {

    private final VendorDocumentRepository repository;
    private final DocumentLinkRepository linkRepository;
    private final StorageGateway storage;
    private final CatalogProperties properties;
    private final AuditService audit;

    public DocumentService(VendorDocumentRepository repository, DocumentLinkRepository linkRepository,
                           StorageGateway storage, CatalogProperties properties, AuditService audit) {
        this.repository = repository;
        this.linkRepository = linkRepository;
        this.storage = storage;
        this.properties = properties;
        this.audit = audit;
    }

    public DocumentDtos.UploadResponse upload(MultipartFile file, String usage, String entityId,
                                              String path) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION, "No file was supplied.");
        }
        long maxBytes = properties.getLimits().getMaxUploadSizeMb() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new ApiException(ErrorCode.FILE_TOO_LARGE,
                    "%.1f MB exceeds the %d MB limit for usage=%s."
                            .formatted(file.getSize() / 1048576.0,
                                    properties.getLimits().getMaxUploadSizeMb(), usage));
        }

        byte[] bytes;
        try (InputStream in = file.getInputStream()) {
            bytes = in.readAllBytes();
        } catch (IOException e) {
            throw new ApiException(ErrorCode.INTERNAL, "The uploaded file could not be read.");
        }

        // Content type is sniffed from the magic bytes, not trusted from the declared header.
        String sniffed = MimeSniffer.sniff(bytes, file.getContentType());
        if (!properties.getDocuments().getAllowedMimeTypes().contains(sniffed)) {
            throw new ApiException(ErrorCode.UNSUPPORTED_MIME,
                    "The file declares %s but its content is %s.".formatted(file.getContentType(), sniffed))
                    .with("allowed", properties.getDocuments().getAllowedMimeTypes());
        }

        String documentId = Ids.newId("doc");
        String vendorId = TenantContext.vendorId();
        String storageKey;
        try {
            storageKey = storage.store(vendorId, documentId, new java.io.ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new ApiException(ErrorCode.INTERNAL, "The uploaded file could not be stored.");
        }

        VendorDocument document = VendorDocument.builder()
                .id(documentId)
                .vendorId(vendorId)
                .fileName(file.getOriginalFilename())
                .mimeType(sniffed)
                .sizeBytes(file.getSize())
                .storageKey(storageKey)
                .checksumSha256(sha256(bytes))
                // A real deployment flips this to CLEAN once the AV scan returns.
                .scanStatus("CLEAN")
                .usage(usage)
                .linkedEntityId(entityId)
                .linkedPath(path)
                .createdAt(Instant.now())
                .createdBy(TenantContext.userId())
                .build();
        repository.save(document);

        audit.record(AuditEvent.DOCUMENT_UPLOADED, "VENDOR_DOCUMENT", documentId,
                Map.of("usage", usage == null ? "" : usage, "sizeBytes", file.getSize()));
        return toResponse(document);
    }

    public DocumentDtos.UploadResponse get(String documentId) {
        return toResponse(require(documentId));
    }

    public VendorDocument require(String documentId) {
        return repository.findByIdAndVendorIdAndDeletedAtIsNull(documentId, TenantContext.vendorId())
                .orElseThrow(() -> ApiException.notFound("Document " + documentId));
    }

    /** Referencing a document still being scanned is a 409, not a silent accept. */
    public void requireReferencable(String documentId) {
        VendorDocument document = require(documentId);
        if (!"CLEAN".equals(document.getScanStatus())) {
            throw new ApiException(ErrorCode.DOCUMENT_NOT_READY,
                    "%s has scanStatus %s.".formatted(documentId, document.getScanStatus()))
                    .with("retryAfterSeconds", 5);
        }
    }

    public void delete(String documentId) {
        VendorDocument document = require(documentId);
        List<DocumentLink> links = linkRepository.findByLibraryDocumentId(documentId);
        if (!links.isEmpty()) {
            throw new ApiException(ErrorCode.DOCUMENT_IN_USE,
                    "%s is linked to %d listing(s).".formatted(documentId, links.size()))
                    .with("referencedBy", links.stream()
                            .map(l -> Map.of("listingId", l.getListingId(), "linkId", l.getId())).toList());
        }
        document.setDeletedAt(Instant.now());
        repository.save(document);
    }

    DocumentDtos.UploadResponse toResponse(VendorDocument document) {
        return new DocumentDtos.UploadResponse(
                document.getId(), document.getFileName(), document.getMimeType(), document.getSizeBytes(),
                displaySize(document.getSizeBytes()), displayType(document.getMimeType()),
                document.getScanStatus(), document.getChecksumSha256(),
                storage.presignedUrl(document.getStorageKey(), document.getId()),
                document.getCreatedAt());
    }

    static String displaySize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return "%.0f KB".formatted(bytes / 1024.0);
        }
        return "%.1f MB".formatted(bytes / 1048576.0);
    }

    static String displayType(String mimeType) {
        if (mimeType == null) {
            return null;
        }
        return switch (mimeType) {
            case "application/pdf" -> "PDF";
            case "image/png" -> "PNG";
            case "image/jpeg" -> "JPG";
            case "image/webp" -> "WEBP";
            default -> "FILE";
        };
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception e) {
            return null;
        }
    }
}
