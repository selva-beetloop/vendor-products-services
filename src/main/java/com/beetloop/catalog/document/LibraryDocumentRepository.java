package com.beetloop.catalog.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface LibraryDocumentRepository extends MongoRepository<LibraryDocument, String> {

    Optional<LibraryDocument> findByIdAndVendorId(String id, String vendorId);

    Page<LibraryDocument> findByVendorId(String vendorId, Pageable pageable);

    Page<LibraryDocument> findByVendorIdAndKind(String vendorId, String kind, Pageable pageable);

    List<LibraryDocument> findByVendorId(String vendorId);

    List<LibraryDocument> findByIdIn(List<String> ids);
}
