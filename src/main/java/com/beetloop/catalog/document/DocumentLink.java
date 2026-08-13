package com.beetloop.catalog.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** The "upload once, link to many" join. selectionId null means the link applies lab-wide. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "document_links")
@CompoundIndex(name = "unique_link",
        def = "{'listingId': 1, 'selectionId': 1, 'libraryDocumentId': 1}", unique = true)
@CompoundIndex(name = "by_library_doc", def = "{'libraryDocumentId': 1}")
public class DocumentLink {

    @Id
    private String id;

    private String vendorId;
    private String listingId;
    private String selectionId;
    private String libraryDocumentId;

    /** ACCREDITATION | CERTIFICATION | SUPPORT_DOC */
    private String linkType;

    private String applicability;
    private Instant createdAt;
}
