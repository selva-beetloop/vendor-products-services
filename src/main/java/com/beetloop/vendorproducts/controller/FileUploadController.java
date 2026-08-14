package com.beetloop.vendorproducts.controller;

import com.beetloop.vendorproducts.domain.StoredFile;
import com.beetloop.vendorproducts.storage.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

/** Upload endpoint behind every file/image control in the wizard. */
@RestController
@RequestMapping("/api/vendor/uploads")
@Tag(name = "Uploads", description = "File and image uploads for spec attachments, compliance documents "
        + "and marketplace images.")
public class FileUploadController {

    private final FileStorageService storageService;

    public FileUploadController(FileStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a file",
            description = "Returns a stable reference the frontend attaches to a spec parameter, a "
                    + "compliance document row or an image field. Pass a `module` containing 'image' to "
                    + "switch validation to image file types.")
    public ResponseEntity<UploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "module", required = false, defaultValue = "product-document") String module,
            @RequestParam(value = "referenceId", required = false) String referenceId,
            @RequestHeader(value = "X-USER-ID", required = false) String userId) {
        StoredFile stored = storageService.store(file, module, referenceId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new UploadResponse(
                stored.getId().toString(),
                stored.getOriginalFilename(),
                stored.getContentType(),
                stored.getSizeBytes(),
                "/api/vendor/uploads/" + stored.getId(),
                stored.getModule(),
                stored.getCreatedAt()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Download a previously uploaded file")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        StoredFile stored = storageService.metadata(id);
        Resource resource = new PathResource(storageService.resolve(stored));
        String contentType = stored.getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : stored.getContentType();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + stored.getOriginalFilename() + "\"")
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
