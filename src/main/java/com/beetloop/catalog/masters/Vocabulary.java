package com.beetloop.catalog.masters;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * One option list. Supports the cascades (Industry -> Sector, Category -> Sub-category, the whole
 * Blend 1.3-1.7 chain) and the open-ended role sets behind "More Roles".
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "vocabularies")
public class Vocabulary {

    @Id
    private String id;

    @Indexed(unique = true)
    private String code;

    private String label;

    /** null for a flat list. */
    @Indexed
    private String parentVocabularyCode;

    @Builder.Default
    private String scope = "GLOBAL";

    private String categoryCode;

    private List<Option> options;

    private int version;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option {
        private String code;
        private String label;
        /** Scopes this option to a parent value, e.g. SECTOR options carry parentCode = INDUSTRY code. */
        private String parentCode;
        private int order;
        @Builder.Default
        private boolean active = true;
        /** false for roles hidden behind the "More Roles" overflow card. */
        @Builder.Default
        private boolean primary = true;
    }
}
