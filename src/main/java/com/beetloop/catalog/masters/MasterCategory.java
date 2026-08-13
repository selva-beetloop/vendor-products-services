package com.beetloop.catalog.masters;

import com.beetloop.catalog.shared.model.ListingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/** The cards on "List a Product" / "List a Service". */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "master_categories")
public class MasterCategory {

    @Id
    private String id;

    private ListingType type;
    private String code;
    private String title;
    private String description;
    private boolean live;
    private List<String> examples;
    private String buttonLabel;
    private String escapeHatchLabel;
    private List<String> commonFor;
    private List<String> searchFacets;
    private int order;

    // Services only.
    private Integer outerSteps;
    private Integer configurationSubSteps;
    /** "Form: 8 steps" printed on the card is marketing copy - do not build against it. */
    private String marketingStepClaim;
}
