package com.beetloop.vendorproducts.repository;

import com.beetloop.vendorproducts.domain.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {
}
