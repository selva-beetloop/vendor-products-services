package com.beetloop.vendorproducts.catalogue.repository;

import com.beetloop.vendorproducts.catalogue.domain.CatalogueKind;
import com.beetloop.vendorproducts.catalogue.domain.CatalogueStatus;
import com.beetloop.vendorproducts.catalogue.domain.CommercialMaster;
import com.beetloop.vendorproducts.catalogue.domain.ScientificMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommercialMasterRepository extends JpaRepository<CommercialMaster, UUID> {

    Optional<CommercialMaster> findByCode(String code);

    boolean existsByCode(String code);

    Optional<CommercialMaster> findByScientificMasterAndGradeKey(ScientificMaster scientificMaster, String gradeKey);

    List<CommercialMaster> findByScientificMaster_Id(UUID scientificMasterId);

    @Query("""
            SELECT c FROM CommercialMaster c
            JOIN c.scientificMaster s
            WHERE (:kind IS NULL OR c.kind = :kind)
              AND (:category IS NULL OR c.category = :category)
              AND (:statuses IS NULL OR c.status IN :statuses)
              AND (:search IS NULL
                   OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(c.assay, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(s.casNumber, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<CommercialMaster> search(@Param("kind") CatalogueKind kind,
                                  @Param("category") String category,
                                  @Param("statuses") Collection<CatalogueStatus> statuses,
                                  @Param("search") String search,
                                  Pageable pageable);
}
