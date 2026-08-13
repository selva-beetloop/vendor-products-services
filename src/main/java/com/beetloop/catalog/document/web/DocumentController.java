package com.beetloop.catalog.document.web;

import com.beetloop.catalog.document.DocumentDtos;
import com.beetloop.catalog.document.DocumentLibraryService;
import com.beetloop.catalog.document.DocumentService;
import com.beetloop.catalog.shared.api.ApiResponse;
import com.beetloop.catalog.shared.api.PageMeta;
import com.beetloop.catalog.shared.api.PagedResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "Documents", description = "Uploads and the shared accreditation / certification library")
@RestController
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentLibraryService libraryService;

    public DocumentController(DocumentService documentService, DocumentLibraryService libraryService) {
        this.documentService = documentService;
        this.libraryService = libraryService;
    }

    @PostMapping(value = "/vendor/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentDtos.UploadResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String usage,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String path) {
        return ResponseEntity.status(201)
                .body(ApiResponse.of(documentService.upload(file, usage, entityId, path)));
    }

    @GetMapping("/vendor/documents/{documentId}")
    public ApiResponse<DocumentDtos.UploadResponse> get(@PathVariable String documentId) {
        return ApiResponse.of(documentService.get(documentId));
    }

    @DeleteMapping("/vendor/documents/{documentId}")
    public ResponseEntity<Void> delete(@PathVariable String documentId) {
        documentService.delete(documentId);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ library

    @GetMapping("/vendor/document-library")
    public PagedResponse<DocumentDtos.LibraryDocumentResponse> list(
            @RequestParam(required = false) String kind,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<DocumentDtos.LibraryDocumentResponse> result =
                libraryService.list(kind, PageRequest.of(page, size));
        return PagedResponse.of(result.getContent(), PageMeta.of(result),
                Map.of("counts", libraryService.counts()));
    }

    /** Add Accreditation / Add Certification / Add Support Doc all land here. */
    @PostMapping("/vendor/document-library")
    public ResponseEntity<Map<String, Object>> create(
            @Valid @RequestBody DocumentDtos.LibraryDocumentRequest request) {
        return ResponseEntity.status(201).body(body(libraryService.create(request)));
    }

    @GetMapping("/vendor/document-library/{libraryDocumentId}")
    public ApiResponse<DocumentDtos.LibraryDocumentResponse> getLibraryDocument(
            @PathVariable String libraryDocumentId) {
        return ApiResponse.of(libraryService.get(libraryDocumentId));
    }

    @PutMapping("/vendor/document-library/{libraryDocumentId}")
    public Map<String, Object> update(@PathVariable String libraryDocumentId,
                                      @Valid @RequestBody DocumentDtos.LibraryDocumentRequest request) {
        return body(libraryService.update(libraryDocumentId, request));
    }

    @DeleteMapping("/vendor/document-library/{libraryDocumentId}")
    public ResponseEntity<Void> deleteLibraryDocument(@PathVariable String libraryDocumentId) {
        libraryService.delete(libraryDocumentId);
        return ResponseEntity.noContent().build();
    }

    /** rejectedFields and warnings sit beside data, matching the save endpoints. */
    private Map<String, Object> body(DocumentLibraryService.CreateResult result) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data", result.document());
        body.put("rejectedFields", result.rejectedFields());
        body.put("warnings", result.warnings());
        return body;
    }
}
