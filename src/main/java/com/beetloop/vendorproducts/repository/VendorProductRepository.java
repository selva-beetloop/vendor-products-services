package com.beetloop.vendorproducts.repository;

import com.beetloop.vendorproducts.domain.ProductCategory;
import com.beetloop.vendorproducts.domain.ProductStatus;
import com.beetloop.vendorproducts.domain.VendorProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface VendorProductRepository extends MongoRepository<VendorProduct, UUID>, VendorProductRepositoryCustom {

    List<VendorProduct> findByCommercialMasterId(UUID commercialMasterId);

    List<VendorProduct> findByVendorIdAndCommercialMasterIdAndStatusNot(
            String vendorId, UUID commercialMasterId, ProductStatus rejected);

    default List<VendorProduct> findActiveByVendorAndCommercialMaster(
            String vendorId, UUID commercialMasterId, ProductStatus rejected) {
        return findByVendorIdAndCommercialMasterIdAndStatusNot(vendorId, commercialMasterId, rejected);
    }
}

interface VendorProductRepositoryCustom {
    Page<VendorProduct> search(String vendorId,
                               ProductCategory category,
                               ProductStatus status,
                               Collection<ProductStatus> statuses,
                               String search,
                               Pageable pageable);
}
