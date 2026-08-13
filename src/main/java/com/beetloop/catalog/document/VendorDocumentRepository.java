package com.beetloop.catalog.document;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface VendorDocumentRepository extends MongoRepository<VendorDocument, String> {

    Optional<VendorDocument> findByIdAndVendorIdAndDeletedAtIsNull(String id, String vendorId);

    List<VendorDocument> findByVendorIdAndDeletedAtIsNull(String vendorId);
}
