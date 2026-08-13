package com.beetloop.catalog.document;

import com.beetloop.catalog.shared.error.ApiException;
import com.beetloop.catalog.shared.tenant.TenantContext;
import com.beetloop.catalog.shared.util.Ids;
import com.beetloop.catalog.template.DerivationService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The join behind "upload once, link to many". */
@Service
public class DocumentLinkService {

    private final DocumentLinkRepository repository;
    private final LibraryDocumentRepository libraryRepository;
    private final DocumentLibraryService libraryService;
    private final DerivationService derivation;

    public DocumentLinkService(DocumentLinkRepository repository,
                               LibraryDocumentRepository libraryRepository,
                               DocumentLibraryService libraryService, DerivationService derivation) {
        this.repository = repository;
        this.libraryRepository = libraryRepository;
        this.libraryService = libraryService;
        this.derivation = derivation;
    }

    public DocumentDtos.DocumentLinkResponse link(String listingId,
                                                  DocumentDtos.DocumentLinkRequest request) {
        LibraryDocument document = libraryService.require(request.libraryDocumentId());
        DocumentLink link = DocumentLink.builder()
                .id(Ids.newId("lnk"))
                .vendorId(TenantContext.vendorId())
                .listingId(listingId)
                .selectionId(request.selectionId())
                .libraryDocumentId(request.libraryDocumentId())
                .linkType(request.linkType())
                .applicability(request.selectionId() == null ? "LAB_WIDE" : "SERVICE_SPECIFIC")
                .createdAt(Instant.now())
                .build();
        repository.save(link);
        return toResponse(link);
    }

    public List<DocumentDtos.DocumentLinkResponse> list(String listingId) {
        return repository.findByListingId(listingId).stream().map(this::toResponse).toList();
    }

    public List<DocumentLink> raw(String listingId) {
        return repository.findByListingId(listingId);
    }

    public void unlink(String listingId, String linkId) {
        DocumentLink link = repository.findById(linkId)
                .filter(l -> l.getListingId().equals(listingId))
                .orElseThrow(() -> ApiException.notFound("Link " + linkId));
        repository.delete(link);
    }

    /**
     * Per-service document rollup for the step-3 table, plus the expiry count the UI never computes.
     */
    public Map<String, Object> rollup(String listingId) {
        List<DocumentLink> links = repository.findByListingId(listingId);
        List<LibraryDocument> documents = loadAll(links);

        Map<String, LibraryDocument> byId = new LinkedHashMap<>();
        documents.forEach(d -> byId.put(d.getId(), d));

        int accreditations = 0;
        int certifications = 0;
        int supportDocs = 0;
        int expired = 0;
        int expiringSoon = 0;
        for (DocumentLink link : links) {
            switch (link.getLinkType()) {
                case "ACCREDITATION" -> accreditations++;
                case "CERTIFICATION" -> certifications++;
                default -> supportDocs++;
            }
            LibraryDocument document = byId.get(link.getLibraryDocumentId());
            if (document == null) {
                continue;
            }
            String status = derivation.expiryStatus(document.getExpiryDate());
            if ("EXPIRED".equals(status)) {
                expired++;
            } else if ("EXPIRING_SOON".equals(status)) {
                expiringSoon++;
            }
        }
        Map<String, Object> counters = new LinkedHashMap<>();
        counters.put("accreditations", accreditations);
        counters.put("certifications", certifications);
        counters.put("supportDocs", supportDocs);
        counters.put("expired", expired);
        counters.put("expiringSoon", expiringSoon);
        return counters;
    }

    /** Every expired link, as blocking errors for the submit gate. */
    public List<LibraryDocument> expiredDocuments(String listingId) {
        List<DocumentLink> links = repository.findByListingId(listingId);
        return loadAll(links).stream()
                .filter(d -> "EXPIRED".equals(derivation.expiryStatus(d.getExpiryDate())))
                .toList();
    }

    private List<LibraryDocument> loadAll(List<DocumentLink> links) {
        if (links.isEmpty()) {
            return List.of();
        }
        return libraryRepository.findByIdIn(links.stream()
                .map(DocumentLink::getLibraryDocumentId).distinct().toList());
    }

    private DocumentDtos.DocumentLinkResponse toResponse(DocumentLink link) {
        return new DocumentDtos.DocumentLinkResponse(link.getId(), link.getListingId(),
                link.getSelectionId(), link.getLibraryDocumentId(), link.getLinkType(),
                link.getApplicability());
    }
}
