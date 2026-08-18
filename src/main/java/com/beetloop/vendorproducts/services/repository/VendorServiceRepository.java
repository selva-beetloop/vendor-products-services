package com.beetloop.vendorproducts.services.repository;

import com.beetloop.vendorproducts.services.domain.ServiceCategory;
import com.beetloop.vendorproducts.services.domain.ServiceStatus;
import com.beetloop.vendorproducts.services.domain.VendorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface VendorServiceRepository extends JpaRepository<VendorService, UUID> {

    /**
     * Backs GET /services. Mirrors the listing page's own controls: the search box
     * matches exactly three fields — name, sku and category — and deliberately not
     * serviceType, deliveryMode, region or status, even though those are visible
     * columns. Filters are applied on the owning batch where they belong to it.
     */
    @Query("""
            SELECT s FROM VendorService s
            JOIN s.batch b
            WHERE (:vendorId IS NULL OR b.vendorId = :vendorId)
              AND (:category IS NULL OR b.category = :category)
              AND (:status IS NULL OR b.status = :status)
              AND (:search = ''
                   OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(s.sku) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(s.categoryLabel) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<VendorService> search(@Param("vendorId") String vendorId,
                               @Param("category") ServiceCategory category,
                               @Param("status") ServiceStatus status,
                               @Param("search") String search,
                               Pageable pageable);
}
