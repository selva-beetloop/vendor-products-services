package com.beetloop.catalog.document;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DocumentLinkRepository extends MongoRepository<DocumentLink, String> {

    List<DocumentLink> findByListingId(String listingId);

    List<DocumentLink> findByLibraryDocumentId(String libraryDocumentId);
}
