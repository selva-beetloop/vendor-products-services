package com.beetloop.vendorproducts.services.repository;

import com.beetloop.vendorproducts.services.domain.VendorServiceBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VendorServiceBatchRepository extends JpaRepository<VendorServiceBatch, UUID> {
}
