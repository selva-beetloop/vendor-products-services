package com.beetloop.catalog.servicelisting;

import com.beetloop.catalog.servicelisting.model.ServiceListing;
import com.beetloop.catalog.shared.model.Lifecycle;
import com.beetloop.catalog.shared.model.QcStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceListingRepository extends MongoRepository<ServiceListing, String> {

    Optional<ServiceListing> findByIdAndVendorIdAndDeletedAtIsNull(String id, String vendorId);

    Page<ServiceListing> findByVendorIdAndDeletedAtIsNull(String vendorId, Pageable pageable);

    List<ServiceListing> findByVendorIdAndDeletedAtIsNull(String vendorId);

    long countByVendorIdAndDeletedAtIsNull(String vendorId);

    long countByVendorIdAndLifecycleAndDeletedAtIsNull(String vendorId, Lifecycle lifecycle);

    long countByVendorIdAndQcStatusIsNullAndDeletedAtIsNull(String vendorId);

    long countByVendorIdAndQcStatusInAndDeletedAtIsNull(String vendorId, List<QcStatus> statuses);
}
