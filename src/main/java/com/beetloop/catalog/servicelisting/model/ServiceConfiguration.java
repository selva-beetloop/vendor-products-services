package com.beetloop.catalog.servicelisting.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A service listing holds N SELECTED SERVICES, each configured independently through its own
 * sub-step sequence - 3 for Consultancy, 5 for Lab Testing / Contract Mfg / Agro Cluster, 11 for CRO.
 * That is the biggest structural difference from products, where a listing holds N variants of one
 * thing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceConfiguration {

    private String configurationId;
    private String selectionId;
    private String masterServiceId;
    private String requestCode;
    private String name;
    private String serviceType;
    private String source;

    @Builder.Default
    private Map<String, Object> sections = new LinkedHashMap<>();

    /** DERIVED: NOT_CONFIGURED | IN_PROCESS | CONFIGURED. */
    @Builder.Default
    private String configurationStatus = "NOT_CONFIGURED";

    private Instant configuredAt;
    private int completionPercent;

    private Instant createdAt;
    private Instant updatedAt;

    @SuppressWarnings("unchecked")
    public Map<String, Object> section(String dataKey) {
        Object value = sections == null ? null : sections.get(dataKey);
        return value instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
    }
}
