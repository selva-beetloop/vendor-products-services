package com.beetloop.vendorproducts.storage;

import com.beetloop.vendorproducts.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns a {@code data:&lt;mime&gt;;base64,…} string into a stored file reference.
 *
 * <p>The wizards embed images and PDFs as data URIs inside JSON. Persisting the
 * URI itself blows past Tomcat / Mongo size limits and is the cause of the
 * document-upload 500s. Decode, store through {@link FileStorage}, return a
 * stable URL; on failure drop the bytes rather than keep the data URI.
 */
@Component
public class DataUriPersister {

    private static final Logger log = LoggerFactory.getLogger(DataUriPersister.class);
    private static final Pattern DATA_URI = Pattern.compile(
            "^data:([^;]+);base64,(.+)$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final FileStorage storage;
    private final CurrentUser currentUser;

    public DataUriPersister(FileStorage storage, CurrentUser currentUser) {
        this.storage = storage;
        this.currentUser = currentUser;
    }

    public record StoredRef(String id, String url, String fileName) {
    }

    public boolean isDataUri(String value) {
        return value != null && value.regionMatches(true, 0, "data:", 0, 5);
    }

    /**
     * @return a stored reference when {@code value} is a data URI and storage
     *         succeeded; {@code null} when the value is not a data URI; an empty
     *         optional-style {@code StoredRef} with null id when storage failed
     *         (caller should clear the data URI so it is not persisted).
     */
    public StoredRef persist(String value, String fileName, String module) {
        if (!isDataUri(value)) {
            return null;
        }
        Matcher matcher = DATA_URI.matcher(value.trim());
        if (!matcher.matches()) {
            log.warn("Ignoring malformed data URI ({} chars)", value.length());
            return new StoredRef(null, null, fileName);
        }
        String contentType = matcher.group(1).trim();
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(matcher.group(2).replaceAll("\\s", ""));
        } catch (IllegalArgumentException e) {
            log.warn("Ignoring undecodable data URI: {}", e.getMessage());
            return new StoredRef(null, null, fileName);
        }
        String original = resolveFileName(fileName, contentType);
        try {
            String uploadedBy;
            try {
                uploadedBy = currentUser.userId();
            } catch (RuntimeException ignored) {
                uploadedBy = "vendor";
            }
            FileStorage.StoredUpload stored = storage.store(
                    new BytesMultipartFile(original, contentType, bytes),
                    module,
                    null,
                    uploadedBy);
            return new StoredRef(stored.id(), stored.url(), stored.fileName());
        } catch (RuntimeException e) {
            log.warn("Failed to persist embedded file '{}': {}", original, e.getMessage());
            return new StoredRef(null, null, original);
        }
    }

    private static String resolveFileName(String fileName, String contentType) {
        if (fileName != null && !fileName.isBlank() && fileName.contains(".")) {
            return fileName;
        }
        String ext = extensionFor(contentType);
        String base = fileName == null || fileName.isBlank() ? "upload" : fileName;
        return ext.isEmpty() ? base : base + "." + ext;
    }

    private static String extensionFor(String contentType) {
        if (contentType == null) {
            return "";
        }
        String type = contentType.toLowerCase(Locale.ROOT);
        return switch (type) {
            case "application/pdf" -> "pdf";
            case "image/png" -> "png";
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            case "application/msword" -> "doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx";
            default -> "";
        };
    }
}
