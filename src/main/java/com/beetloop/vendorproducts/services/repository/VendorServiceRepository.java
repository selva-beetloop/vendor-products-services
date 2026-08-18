package com.beetloop.vendorproducts.services.repository;

import com.beetloop.vendorproducts.services.domain.ServiceCategory;
import com.beetloop.vendorproducts.services.domain.ServiceStatus;
import com.beetloop.vendorproducts.services.domain.VendorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VendorServiceRepository {

    /**
     * Backs GET /services. Search matches name, sku and category label on the
     * embedded service items; filters apply to the owning batch.
     */
    Page<VendorService> search(String vendorId,
                               ServiceCategory category,
                               ServiceStatus status,
                               String search,
                               Pageable pageable);
}
