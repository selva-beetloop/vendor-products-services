package com.beetloop.vendorproducts.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

/** Upload/download used by {@code /api/vendor/uploads}. */
public interface FileStorage {

    StoredUpload store(MultipartFile file, String module, String referenceId, String uploadedBy);

    StoredUpload metadata(String id);

    Resource content(String id);

    record StoredUpload(
            String id,
            String fileName,
            String contentType,
            long sizeBytes,
            String module,
            Instant uploadedAt) {
        public String url() {
            return "/api/vendor/uploads/" + id;
        }
    }
}
