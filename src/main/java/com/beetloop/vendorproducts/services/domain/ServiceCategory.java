package com.beetloop.vendorproducts.services.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The five service categories offered on the "List a Service" chooser.
 *
 * <p>Stage counts were read from each category's own stepper data rather than
 * from the spec documents, whose counts were guidance and turned out to be wrong
 * for four of the five: Lab Testing, Consultancy and Contract Manufacturer each
 * have 4 top-level stages (not 5/3/5), Agro-Processing has 4 (not 5), and only
 * CRO's 11 was correct.
 *
 * <p>Note also that Contract Manufacturer's third stage is <em>Compliance</em>,
 * not "Accreditations &amp; Certifications" — a genuine structural difference.
 */
public enum ServiceCategory {

    LAB_TESTING("lab-testing", "Lab Testing Services", "lab-testing", 4),
    CONSULTANCY("consultancy", "Consultancy Services", "consultancy", 4),
    CONTRACT_MANUFACTURER("contract-manufacturer",
            "Contract Manufacturer · Co-Pack · Private Label", "manufacturing-logistics", 4),
    AGRO_PROCESSING("agro-processing",
            "Agro-Processing Cluster · PMKSY · CDP · ODOP", "facilities-research", 4),
    CRO("cro", "Contract Research Organization (CRO)", "facilities-research", 11);

    private final String id;
    private final String label;
    /** Catalog chip group used by the services listing filter. */
    private final String groupId;
    private final int stageCount;

    ServiceCategory(String id, String label, String groupId, int stageCount) {
        this.id = id;
        this.label = label;
        this.groupId = groupId;
        this.stageCount = stageCount;
    }

    @JsonValue
    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getGroupId() {
        return groupId;
    }

    public int getStageCount() {
        return stageCount;
    }

    @JsonCreator
    public static ServiceCategory from(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String needle = raw.trim().toLowerCase().replace('_', '-');
        for (ServiceCategory category : values()) {
            if (category.id.equals(needle)
                    || category.name().toLowerCase().replace('_', '-').equals(needle)) {
                return category;
            }
        }
        throw new IllegalArgumentException(
                "Unknown service category '" + raw + "'. Expected one of: lab-testing, consultancy, "
                        + "contract-manufacturer, agro-processing, cro");
    }
}
