package com.beetloop.catalog.facility;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface VendorFacilityRepository extends MongoRepository<VendorFacility, String> {
    List<VendorFacility> findByVendorId(String vendorId);

    List<VendorFacility> findByVendorIdAndVerified(String vendorId, boolean verified);

    Optional<VendorFacility> findByIdAndVendorId(String id, String vendorId);
}
