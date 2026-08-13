package com.beetloop.catalog.masters;

import com.beetloop.catalog.shared.model.ListingType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MasterCatalogRepository extends MongoRepository<MasterCatalogEntry, String> {
    List<MasterCatalogEntry> findByTypeAndCategoryCode(ListingType type, String categoryCode);
}
