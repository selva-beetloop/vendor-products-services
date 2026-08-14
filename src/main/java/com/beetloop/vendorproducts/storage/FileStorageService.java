package com.beetloop.vendorproducts.storage;

import com.beetloop.vendorproducts.domain.StoredFile;
import com.beetloop.vendorproducts.dto.ApiError;
import com.beetloop.vendorproducts.exception.ResourceNotFoundException;
import com.beetloop.vendorproducts.exception.ValidationException;
import com.beetloop.vendorproducts.repository.StoredFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Local-disk storage behind {@code POST /uploads}. Enforces the same file rules
 * the upload controls in the wizard advertise (document types for compliance /
 * spec attachments, image types for marketplace images).
 */
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "csv", "txt");
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "png", "jpg", "jpeg", "webp", "gif", "svg");

    private final StoredFileRepository storedFileRepository;
    private final Path root;
    private final long maxBytes;

    public FileStorageService(StoredFileRepository storedFileRepository,
                              @Value("${app.storage.root:./data/uploads}") String storageRoot,
                              @Value("${app.storage.max-file-size-bytes:10485760}") long maxBytes) {
        this.storedFileRepository = storedFileRepository;
        this.root = Paths.get(storageRoot).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create upload directory " + this.root, e);
        }
    }

    public StoredFile store(MultipartFile file, String module, String referenceId, String uploadedBy) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Upload failed",
                    List.of(new ApiError.FieldError("file", "Select a file to upload", null)));
        }
        if (file.getSize() > maxBytes) {
            throw new ValidationException("Upload failed",
                    List.of(new ApiError.FieldError("file",
                            "File exceeds the " + (maxBytes / (1024 * 1024)) + " MB limit", file.getSize())));
        }

        String original = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename());
        if (original.contains("..")) {
            throw new ValidationException("Upload failed",
                    List.of(new ApiError.FieldError("file", "Invalid file name", original)));
        }

        String extension = StringUtils.getFilenameExtension(original);
        String normalised = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        boolean isImageModule = module != null && module.toLowerCase(Locale.ROOT).contains("image");
        Set<String> allowed = isImageModule ? ALLOWED_IMAGE_TYPES : ALLOWED_DOCUMENT_TYPES;
        if (!allowed.contains(normalised)) {
            throw new ValidationException("Upload failed",
                    List.of(new ApiError.FieldError("file",
                            "Unsupported file type '." + normalised + "'. Allowed: "
                                    + String.join(", ", allowed), original)));
        }

        UUID id = UUID.randomUUID();
        String storedName = id + (normalised.isEmpty() ? "" : "." + normalised);
        Path target = root.resolve(storedName).normalize();
        if (!target.startsWith(root)) {
            throw new ValidationException("Upload failed",
                    List.of(new ApiError.FieldError("file", "Invalid file path", original)));
        }

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file " + original, e);
        }

        StoredFile record = new StoredFile();
        record.setId(id);
        record.setOriginalFilename(original);
        record.setContentType(file.getContentType());
        record.setSizeBytes(file.getSize());
        record.setStoragePath(storedName);
        record.setModule(module);
        record.setReferenceId(referenceId);
        record.setUploadedBy(uploadedBy);
        return storedFileRepository.save(record);
    }

    public StoredFile metadata(UUID id) {
        return storedFileRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.file(id));
    }

    public Path resolve(StoredFile file) {
        Path path = root.resolve(file.getStoragePath()).normalize();
        if (!path.startsWith(root) || !Files.exists(path)) {
            throw ResourceNotFoundException.file(file.getId());
        }
        return path;
    }
}
