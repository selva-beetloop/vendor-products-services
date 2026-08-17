package com.beetloop.vendorproducts.catalogue.repository;

import com.beetloop.vendorproducts.catalogue.domain.CatalogueKind;
import com.beetloop.vendorproducts.catalogue.domain.CatalogueStatus;
import com.beetloop.vendorproducts.catalogue.domain.ScientificMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ScientificMasterRepository extends JpaRepository<ScientificMaster, UUID> {

    Optional<ScientificMaster> findByCode(String code);

    boolean existsByCode(String code);

    @Query("""
            SELECT s FROM ScientificMaster s
            WHERE (:kind IS NULL OR s.kind = :kind)
              AND (:category IS NULL OR s.category = :category)
              AND (:statuses IS NULL OR s.status IN :statuses)
              AND (:search IS NULL
                   OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(s.casNumber, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(s.code) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<ScientificMaster> search(@Param("kind") CatalogueKind kind,
                                  @Param("category") String category,
                                  @Param("statuses") Collection<CatalogueStatus> statuses,
                                  @Param("search") String search,
                                  Pageable pageable);
}
