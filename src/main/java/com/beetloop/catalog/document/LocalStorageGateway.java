package com.beetloop.catalog.document;

import com.beetloop.catalog.config.CatalogProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;

@Component
public class LocalStorageGateway implements StorageGateway {

    private final CatalogProperties properties;

    public LocalStorageGateway(CatalogProperties properties) {
        this.properties = properties;
    }

    @Override
    public String store(String vendorId, String documentId, InputStream content) throws IOException {
        LocalDate today = LocalDate.now();
        // Vendor-scoped key prefix; the vendor's own filename is never part of the key.
        String key = "%s/%d/%02d/%s".formatted(vendorId, today.getYear(), today.getMonthValue(), documentId);
        Path target = Path.of(properties.getStorage().getBaseDir()).resolve(key);
        Files.createDirectories(target.getParent());
        Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        return key;
    }

    @Override
    public String presignedUrl(String storageKey, String documentId) {
        return "%s/%s/content".formatted(properties.getStorage().getPublicBaseUrl(), documentId);
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(Path.of(properties.getStorage().getBaseDir()).resolve(storageKey));
    }
}
