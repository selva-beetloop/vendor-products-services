package com.beetloop.vendorproducts.documentstore;

import com.beetloop.vendorproducts.documentstore.client.DocumentStoreClient;
import com.beetloop.vendorproducts.documentstore.client.DocumentStoreHttpClient;
import com.beetloop.vendorproducts.documentstore.dto.DocumentStoreDocumentDto;
import com.beetloop.vendorproducts.exception.ResourceNotFoundException;
import com.beetloop.vendorproducts.storage.FileStorage;
import com.beetloop.vendorproducts.storage.UploadRules;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

@Service
@ConditionalOnProperty(name = "app.document-store.enabled", havingValue = "true", matchIfMissing = true)
public class DocumentStoreFileStorage implements FileStorage {

    private final DocumentStoreClient feign;
    private final DocumentStoreHttpClient http;
    private final UploadRules rules;
    private final ObjectMapper objectMapper;

    public DocumentStoreFileStorage(DocumentStoreClient feign,
                                    DocumentStoreHttpClient http,
                                    UploadRules rules,
                                    ObjectMapper objectMapper) {
        this.feign = feign;
        this.http = http;
        this.rules = rules;
        this.objectMapper = objectMapper;
    }

    @Override
    public StoredUpload store(MultipartFile file, String module, String referenceId, String uploadedBy) {
        rules.validate(file, module);
        String original = rules.originalFilename(file);
        String documentType = rules.documentType(module, original);
        String moduleReference = module == null || module.isBlank() ? "vendor-products" : module;
        String ref = referenceId == null || referenceId.isBlank()
                ? (uploadedBy == null || uploadedBy.isBlank() ? "vendor-products" : uploadedBy)
                : referenceId;

        DocumentStoreDocumentDto stored;
        try {
            stored = feign.upload(file, moduleReference, ref, documentType);
        } catch (RuntimeException feignFailed) {
            stored = parseUpload(http.upload(file, moduleReference, ref, documentType)
                    .orElseThrow(() -> new IllegalStateException("Document-store upload returned an empty body",
                            feignFailed)));
        }
        if (stored == null || stored.resolvedId() == null || stored.resolvedId().isBlank()) {
            throw new IllegalStateException("Document-store upload did not return an id");
        }
        String fileName = stored.resolvedFileName() == null ? original : stored.resolvedFileName();
        String contentType = stored.fileType() != null ? stored.fileType() : file.getContentType();
        long size = stored.fileSizeBytes() != null ? stored.fileSizeBytes() : file.getSize();
        Instant uploadedAt = stored.createdAt() != null ? stored.createdAt()
                : stored.uploadedAt() != null ? stored.uploadedAt() : Instant.now();
        return new StoredUpload(stored.resolvedId(), fileName, contentType, size, module, uploadedAt);
    }

    @Override
    public StoredUpload metadata(String id) {
        if (id == null || id.isBlank()) {
            throw ResourceNotFoundException.file(id);
        }
        return new StoredUpload(id, id, MediaType.APPLICATION_OCTET_STREAM_VALUE, -1, null, Instant.now());
    }

    @Override
    public Resource content(String id) {
        try {
            return http.view(id);
        } catch (ResourceNotFoundException ignored) {
            return http.download(id);
        }
    }

    private DocumentStoreDocumentDto parseUpload(String body) {
        try {
            return objectMapper.readValue(body, DocumentStoreDocumentDto.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not parse document-store upload response", ex);
        }
    }
}
