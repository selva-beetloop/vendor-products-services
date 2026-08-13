package com.beetloop.catalog.masters;

import com.beetloop.catalog.shared.model.ListingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

/**
 * The commercial master catalogue behind the per-category search screen. Facets differ per category
 * (Raw Materials: Type/Form/Origin; Finished Goods: Industry/Sector/Category/Stage/Claims; ...),
 * so they live in an open `facets` map rather than fixed columns.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "master_catalog")
@CompoundIndex(name = "type_category", def = "{'type': 1, 'categoryCode': 1}")
public class MasterCatalogEntry {

    @Id
    private String id;

    private ListingType type;
    private String categoryCode;

    @TextIndexed
    private String name;

    @TextIndexed
    private List<String> synonyms;

    private String imageUrl;
    private Map<String, Object> attributes;
    private Map<String, String> facets;
}
