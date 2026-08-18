package com.beetloop.vendorproducts.services.repository;

import com.beetloop.vendorproducts.services.domain.VendorServiceBatch;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface VendorServiceBatchRepository extends MongoRepository<VendorServiceBatch, UUID> {
}
