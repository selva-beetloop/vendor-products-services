package com.beetloop.catalog.product;

import com.beetloop.catalog.product.model.ProductListing;
import com.beetloop.catalog.shared.model.Lifecycle;
import com.beetloop.catalog.shared.model.QcStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProductListingRepository extends MongoRepository<ProductListing, String> {

    /** Every read is vendor-scoped. A cross-vendor id resolves to empty, and the caller turns that into 404. */
    Optional<ProductListing> findByIdAndVendorIdAndDeletedAtIsNull(String id, String vendorId);

    Page<ProductListing> findByVendorIdAndDeletedAtIsNull(String vendorId, Pageable pageable);

    List<ProductListing> findByVendorIdAndDeletedAtIsNull(String vendorId);

    long countByVendorIdAndDeletedAtIsNull(String vendorId);

    long countByVendorIdAndLifecycleAndDeletedAtIsNull(String vendorId, Lifecycle lifecycle);

    long countByVendorIdAndQcStatusIsNullAndDeletedAtIsNull(String vendorId);

    long countByVendorIdAndQcStatusInAndDeletedAtIsNull(String vendorId, List<QcStatus> statuses);
}
