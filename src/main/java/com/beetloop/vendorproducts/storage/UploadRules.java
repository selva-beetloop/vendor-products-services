package com.beetloop.vendorproducts.storage;

import com.beetloop.vendorproducts.dto.ApiError;
import com.beetloop.vendorproducts.exception.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class UploadRules {

    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "csv", "txt");
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "png", "jpg", "jpeg", "webp", "gif", "svg");

    private final long maxBytes;

    public UploadRules(@Value("${app.storage.max-file-size-bytes:10485760}") long maxBytes) {
        this.maxBytes = maxBytes;
    }

    public void validate(MultipartFile file, String module) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Upload failed",
                    List.of(new ApiError.FieldError("file", "Select a file to upload", null)));
        }
        if (file.getSize() > maxBytes) {
            throw new ValidationException("Upload failed",
                    List.of(new ApiError.FieldError("file",
                            "File exceeds the " + (maxBytes / (1024 * 1024)) + " MB limit", file.getSize())));
        }
        String original = originalFilename(file);
        if (original.contains("..")) {
            throw new ValidationException("Upload failed",
                    List.of(new ApiError.FieldError("file", "Invalid file name", original)));
        }
        String extension = extensionOf(original);
        Set<String> allowed = isImageModule(module) ? ALLOWED_IMAGE_TYPES : ALLOWED_DOCUMENT_TYPES;
        if (!allowed.contains(extension)) {
            throw new ValidationException("Upload failed",
                    List.of(new ApiError.FieldError("file",
                            "Unsupported file type '." + extension + "'. Allowed: "
                                    + String.join(", ", allowed), original)));
        }
    }

    public String originalFilename(MultipartFile file) {
        return StringUtils.cleanPath(file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename());
    }

    public String extensionOf(String original) {
        String extension = StringUtils.getFilenameExtension(original);
        return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    }

    public boolean isImageModule(String module) {
        return module != null && module.toLowerCase(Locale.ROOT).contains("image");
    }

    public String documentType(String module, String originalFilename) {
        if (isImageModule(module)) {
            return "IMAGE";
        }
        String extension = extensionOf(originalFilename);
        return extension.isBlank() ? "DOCUMENT" : extension.toUpperCase(Locale.ROOT);
    }
}
