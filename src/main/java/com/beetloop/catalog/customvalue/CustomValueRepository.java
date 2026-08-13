package com.beetloop.catalog.customvalue;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CustomValueRepository extends MongoRepository<CustomValue, String> {

    Optional<CustomValue> findByVendorIdAndFieldKeyAndNormalizedValue(String vendorId, String fieldKey,
                                                                     String normalizedValue);

    List<CustomValue> findByVendorIdAndFieldKey(String vendorId, String fieldKey);

    List<CustomValue> findByVendorId(String vendorId);

    Optional<CustomValue> findByIdAndVendorId(String id, String vendorId);
}
