package com.beetloop.vendorproducts.storage;

import com.beetloop.vendorproducts.domain.StoredFile;
import com.beetloop.vendorproducts.dto.ApiError;
import com.beetloop.vendorproducts.exception.ResourceNotFoundException;
import com.beetloop.vendorproducts.exception.ValidationException;
import com.beetloop.vendorproducts.repository.StoredFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

/**
 * Local-disk storage behind {@code POST /uploads} when document-store is disabled
 * ({@code local-storage} profile or {@code app.document-store.enabled=false}).
 */
@Service
@ConditionalOnProperty(name = "app.document-store.enabled", havingValue = "false")
public class FileStorageService implements FileStorage {

    private final StoredFileRepository storedFileRepository;
    private final UploadRules rules;
    private final Path root;

    public FileStorageService(StoredFileRepository storedFileRepository,
                              UploadRules rules,
                              @Value("${app.storage.root:./data/uploads}") String storageRoot) {
        this.storedFileRepository = storedFileRepository;
        this.rules = rules;
        this.root = Paths.get(storageRoot).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create upload directory " + this.root, e);
        }
    }

    @Override
    public StoredUpload store(MultipartFile file, String module, String referenceId, String uploadedBy) {
        rules.validate(file, module);
        String original = rules.originalFilename(file);
        String extension = rules.extensionOf(original);

        UUID id = UUID.randomUUID();
        String storedName = id + (extension.isEmpty() ? "" : "." + extension);
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
        StoredFile saved = storedFileRepository.save(record);
        return toUpload(saved);
    }

    @Override
    public StoredUpload metadata(String id) {
        return toUpload(load(id));
    }

    @Override
    public Resource content(String id) {
        StoredFile stored = load(id);
        Path path = root.resolve(stored.getStoragePath()).normalize();
        if (!path.startsWith(root) || !Files.exists(path)) {
            throw ResourceNotFoundException.file(stored.getId());
        }
        return new PathResource(path);
    }

    private StoredFile load(String id) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw ResourceNotFoundException.file(id);
        }
        return storedFileRepository.findById(uuid).orElseThrow(() -> ResourceNotFoundException.file(id));
    }

    private static StoredUpload toUpload(StoredFile stored) {
        return new StoredUpload(
                stored.getId().toString(),
                stored.getOriginalFilename(),
                stored.getContentType(),
                stored.getSizeBytes(),
                stored.getModule(),
                stored.getCreatedAt());
    }
}
