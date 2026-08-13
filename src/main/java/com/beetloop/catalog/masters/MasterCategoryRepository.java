package com.beetloop.catalog.masters;

import com.beetloop.catalog.shared.model.ListingType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface MasterCategoryRepository extends MongoRepository<MasterCategory, String> {
    List<MasterCategory> findByTypeOrderByOrderAsc(ListingType type);

    Optional<MasterCategory> findByCode(String code);
}
