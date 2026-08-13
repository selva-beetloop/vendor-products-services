package com.beetloop.catalog.servicelisting.model;

import com.beetloop.catalog.shared.model.EntryPath;
import com.beetloop.catalog.shared.model.Lifecycle;
import com.beetloop.catalog.shared.model.QcStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "service_listings")
@CompoundIndex(name = "vendor_updated", def = "{'vendorId': 1, 'deletedAt': 1, 'updatedAt': -1}")
@CompoundIndex(name = "vendor_category_qc", def = "{'vendorId': 1, 'categoryCode': 1, 'qcStatus': 1}")
@CompoundIndex(name = "qc_queue", def = "{'qcStatus': 1, 'submittedAt': 1}")
public class ServiceListing {

    @Id
    private String id;

    @Indexed(unique = true)
    private String code;

    private String vendorId;
    private ServiceCategoryCode categoryCode;
    private String categoryId;
    private EntryPath entryPath;
    private int templateVersion;

    private String currentStep;

    @Builder.Default
    private List<String> completedSteps = new ArrayList<>();

    /** Keyed by step: selectService, configureServices, compliance, review. */
    @Builder.Default
    private Map<String, Object> data = new LinkedHashMap<>();

    @Builder.Default
    private List<ServiceConfiguration> configurations = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> derived = new LinkedHashMap<>();

    @Builder.Default
    private Map<String, Object> search = new LinkedHashMap<>();

    private QcStatus qcStatus;

    @Builder.Default
    private Lifecycle lifecycle = Lifecycle.DRAFT;

    private Instant submittedAt;
    private String submittedBy;
    private Instant publishedAt;
    private int revision;

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;
    @CreatedBy
    private String createdBy;
    @LastModifiedDate
    private Instant updatedAt;
    @LastModifiedBy
    private String updatedBy;

    private Instant deletedAt;

    @SuppressWarnings("unchecked")
    public Map<String, Object> step(String dataKey) {
        Object value = data == null ? null : data.get(dataKey);
        return value instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
    }

    public void putStep(String dataKey, Map<String, Object> value) {
        if (data == null) {
            data = new LinkedHashMap<>();
        }
        data.put(dataKey, value);
    }

    public long versionOrZero() {
        return version == null ? 0L : version;
    }

    public boolean editable() {
        return qcStatus == null || qcStatus == QcStatus.REJECTED;
    }
}
