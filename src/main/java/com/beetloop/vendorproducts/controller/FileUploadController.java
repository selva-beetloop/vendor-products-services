package com.beetloop.vendorproducts.controller;

import com.beetloop.vendorproducts.security.CurrentUser;
import com.beetloop.vendorproducts.storage.FileStorage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

/** Upload endpoint behind every file/image control in the wizard. */
@RestController
@RequestMapping("/api/vendor/uploads")
@Tag(name = "Uploads", description = "File and image uploads for spec attachments, compliance documents "
        + "and marketplace images. Proxies to documents-store unless local-storage is enabled.")
public class FileUploadController {

    private final FileStorage storage;
    private final CurrentUser currentUser;

    public FileUploadController(FileStorage storage, CurrentUser currentUser) {
        this.storage = storage;
        this.currentUser = currentUser;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Upload a file",
            description = "Returns a stable reference the frontend attaches to a spec parameter, a "
                    + "compliance document row or an image field. Pass a `module` containing 'image' to "
                    + "switch validation to image file types.")
    public ResponseEntity<UploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "module", required = false, defaultValue = "product-document") String module,
            @RequestParam(value = "referenceId", required = false) String referenceId) {
        FileStorage.StoredUpload stored = storage.store(file, module, referenceId, currentUser.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new UploadResponse(
                stored.id(),
                stored.fileName(),
                stored.contentType(),
                stored.sizeBytes(),
                stored.url(),
                stored.module(),
                stored.uploadedAt()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VENDOR','QC_ADMIN','QC_USER','INTEL_QC','INTEL_ADMIN')")
    @Operation(summary = "Download a previously uploaded file")
    public ResponseEntity<Resource> download(@PathVariable String id) {
        FileStorage.StoredUpload stored = storage.metadata(id);
        Resource resource = storage.content(id);
        String contentType = stored.contentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : stored.contentType();
        String fileName = stored.fileName() == null ? id : stored.fileName();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(resource);
    }

    public record UploadResponse(
            String id,
            String fileName,
            String contentType,
            long sizeBytes,
            String url,
            String module,
            Instant uploadedAt) {
    }
}
