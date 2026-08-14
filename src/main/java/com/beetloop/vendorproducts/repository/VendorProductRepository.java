package com.beetloop.vendorproducts.repository;

import com.beetloop.vendorproducts.domain.ProductCategory;
import com.beetloop.vendorproducts.domain.ProductStatus;
import com.beetloop.vendorproducts.domain.VendorProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.UUID;

public interface VendorProductRepository extends JpaRepository<VendorProduct, UUID> {

    /**
     * Backs {@code GET /products}. Mirrors the catalog page's controls: free-text
     * search over name/SKU/category, the category chip, the status filter and the
     * vendor scope. Every filter is optional.
     */
    @Query("""
            SELECT p FROM VendorProduct p
            WHERE (:vendorId IS NULL OR p.vendorId = :vendorId)
              AND (:category IS NULL OR p.category = :category)
              AND (:status IS NULL OR p.status = :status)
              AND (:statuses IS NULL OR p.status IN :statuses)
              AND (:search IS NULL
                   OR LOWER(COALESCE(p.name, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(p.sku, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(p.listingCategory, '')) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<VendorProduct> search(@Param("vendorId") String vendorId,
                               @Param("category") ProductCategory category,
                               @Param("status") ProductStatus status,
                               @Param("statuses") Collection<ProductStatus> statuses,
                               @Param("search") String search,
                               Pageable pageable);
}
