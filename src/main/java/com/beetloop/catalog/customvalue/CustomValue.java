package com.beetloop.catalog.customvalue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Replaces the "+ Add" chip values that currently vanish on reload - they append a plain string to
 * React state and never reach localStorage or the network.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "custom_values")
@CompoundIndex(name = "unique_value",
        def = "{'vendorId': 1, 'fieldKey': 1, 'normalizedValue': 1}", unique = true)
public class CustomValue {

    @Id
    private String id;

    private String vendorId;
    private String fieldKey;
    private String vocabularyCode;
    private String value;

    /** trimmed, lower-cased, spaces collapsed - the dedup key. */
    private String normalizedValue;

    @Builder.Default
    private int usageCount = 0;

    private Instant createdAt;
    private String createdBy;
}
