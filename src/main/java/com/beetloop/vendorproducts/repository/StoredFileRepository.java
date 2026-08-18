package com.beetloop.vendorproducts.repository;

import com.beetloop.vendorproducts.domain.StoredFile;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface StoredFileRepository extends MongoRepository<StoredFile, UUID> {
}
