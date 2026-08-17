package com.beetloop.vendorproducts.services.repository;

import com.beetloop.vendorproducts.services.domain.ServiceDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceDocumentRepository extends JpaRepository<ServiceDocument, UUID> {

    List<ServiceDocument> findByServiceIdOrderByPositionAsc(UUID serviceId);
}
