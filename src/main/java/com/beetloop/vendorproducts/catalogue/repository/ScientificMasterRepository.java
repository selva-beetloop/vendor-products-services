package com.beetloop.vendorproducts.catalogue.repository;

import com.beetloop.vendorproducts.catalogue.domain.CatalogueKind;
import com.beetloop.vendorproducts.catalogue.domain.CatalogueStatus;
import com.beetloop.vendorproducts.catalogue.domain.ScientificMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ScientificMasterRepository extends MongoRepository<ScientificMaster, UUID>,
        ScientificMasterRepositoryCustom {

    Optional<ScientificMaster> findByCode(String code);

    boolean existsByCode(String code);

    Page<ScientificMaster> findByStatusIn(Collection<CatalogueStatus> statuses, Pageable pageable);
}

interface ScientificMasterRepositoryCustom {
    Page<ScientificMaster> search(CatalogueKind kind,
                                  String category,
                                  Collection<CatalogueStatus> statuses,
                                  String search,
                                  Pageable pageable);
}
