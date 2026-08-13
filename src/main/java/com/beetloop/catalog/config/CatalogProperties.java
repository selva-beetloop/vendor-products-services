package com.beetloop.catalog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Everything in application.yml under `beetloop.catalog`. */
@Data
@ConfigurationProperties(prefix = "beetloop.catalog")
public class CatalogProperties {

    private Limits limits = new Limits();
    private Save save = new Save();
    private Documents documents = new Documents();
    private Qc qc = new Qc();
    private Idempotency idempotency = new Idempotency();
    private Storage storage = new Storage();
    private Seed seed = new Seed();

    @Data
    public static class Limits {
        private int maxVariantsPerListing = 500;
        private int maxConfigurationsPerListing = 100;
        private int maxOtherImages = 7;
        private int maxVariantImages = 6;
        private int maxTags = 20;
        private int maxUploadSizeMb = 10;
        private int maxBulkUploadRows = 5000;
    }

    @Data
    public static class Save {
        /** PUT /save-all replaces the whole listing, so a stale write is unrecoverable. */
        private boolean requireIfMatchOnSaveAll = true;
        private boolean rejectUnknownDataKeys = true;
    }

    @Data
    public static class Documents {
        private int expiringSoonWindowDays = 30;
        private List<String> allowedMimeTypes = List.of(
                "application/pdf", "image/png", "image/jpeg", "image/webp",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @Data
    public static class Qc {
        private int claimTimeoutMinutes = 120;
    }

    @Data
    public static class Idempotency {
        private int ttlHours = 24;
    }

    @Data
    public static class Storage {
        private String baseDir = "./.storage";
        private String publicBaseUrl = "http://localhost:8080/api/v1/vendor/documents";
    }

    @Data
    public static class Seed {
        private boolean enabled = true;
    }
}
