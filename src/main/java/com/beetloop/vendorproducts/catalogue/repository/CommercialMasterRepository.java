package com.beetloop.vendorproducts.catalogue.repository;

import com.beetloop.vendorproducts.catalogue.domain.CatalogueKind;
import com.beetloop.vendorproducts.catalogue.domain.CatalogueStatus;
import com.beetloop.vendorproducts.catalogue.domain.CommercialMaster;
import com.beetloop.vendorproducts.catalogue.domain.ScientificMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommercialMasterRepository extends MongoRepository<CommercialMaster, UUID>,
        CommercialMasterRepositoryCustom {

    Optional<CommercialMaster> findByCode(String code);

    boolean existsByCode(String code);

    Optional<CommercialMaster> findByScientificMasterAndGradeKey(ScientificMaster scientificMaster, String gradeKey);

    List<CommercialMaster> findByScientificMaster_Id(UUID scientificMasterId);

    Page<CommercialMaster> findByStatusIn(Collection<CatalogueStatus> statuses, Pageable pageable);
}

interface CommercialMasterRepositoryCustom {
    Page<CommercialMaster> search(CatalogueKind kind,
                                  String category,
                                  Collection<CatalogueStatus> statuses,
                                  String search,
                                  Pageable pageable);
}
