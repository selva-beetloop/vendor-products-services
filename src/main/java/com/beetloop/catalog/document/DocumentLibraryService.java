package com.beetloop.catalog.document;

import com.beetloop.catalog.shared.error.ApiException;
import com.beetloop.catalog.shared.error.ErrorCode;
import com.beetloop.catalog.shared.error.RejectedField;
import com.beetloop.catalog.shared.error.Warning;
import com.beetloop.catalog.shared.tenant.TenantContext;
import com.beetloop.catalog.shared.util.DateNormalizer;
import com.beetloop.catalog.shared.util.Ids;
import com.beetloop.catalog.template.DerivationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Backs the three buttons on the service wizard's step 3 - all of which are currently no-ops in the UI. */
@Service
public class DocumentLibraryService {

    private final LibraryDocumentRepository repository;
    private final DocumentLinkRepository linkRepository;
    private final DocumentService documentService;
    private final DateNormalizer dateNormalizer;
    private final DerivationService derivation;

    public DocumentLibraryService(LibraryDocumentRepository repository,
                                  DocumentLinkRepository linkRepository,
                                  DocumentService documentService, DateNormalizer dateNormalizer,
                                  DerivationService derivation) {
        this.repository = repository;
        this.linkRepository = linkRepository;
        this.documentService = documentService;
        this.dateNormalizer = dateNormalizer;
        this.derivation = derivation;
    }

    public record CreateResult(DocumentDtos.LibraryDocumentResponse document,
                               List<RejectedField> rejectedFields,
                               List<Warning> warnings) {
    }

    public CreateResult create(DocumentDtos.LibraryDocumentRequest request) {
        List<RejectedField> rejected = new ArrayList<>();
        List<Warning> warnings = new ArrayList<>();

        if (request.status() != null && !request.status().isBlank()) {
            rejected.add(RejectedField.of("status", RejectedField.DERIVED_FIELD,
                    "Document status is derived from the expiry date and cannot be set by the vendor."));
        }
        if (request.documentId() != null) {
            documentService.requireReferencable(request.documentId());
        }

        Map<String, String> interpretation = new LinkedHashMap<>();
        LocalDate issueDate = normalize(request.issueDate(), "issueDate", interpretation);
        LocalDate expiryDate = normalize(request.expiryDate(), "expiryDate", interpretation);

        Instant now = Instant.now();
        LibraryDocument document = LibraryDocument.builder()
                .id(Ids.newId("lib"))
                .vendorId(TenantContext.vendorId())
                .kind(request.kind())
                .code(request.code())
                .name(request.name())
                .issuingBody(request.issuingBody())
                .referenceNo(request.referenceNo())
                .scope(request.scope())
                .issueDate(issueDate)
                .expiryDate(expiryDate)
                .applicability(request.applicability() == null ? "LAB_WIDE" : request.applicability())
                .documentId(request.documentId())
                .status(derivation.expiryStatus(expiryDate))
                .statusComputedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
        repository.save(document);

        if ("EXPIRED".equals(document.getStatus())) {
            warnings.add(Warning.of(null, "DOCUMENT_EXPIRED",
                    "%s expired on %s. Linking it will block submission."
                            .formatted(document.getName(), document.getExpiryDate())));
        } else if ("EXPIRING_SOON".equals(document.getStatus())) {
            warnings.add(Warning.of(null, "DOCUMENT_EXPIRING_SOON",
                    "%s expires on %s.".formatted(document.getName(), document.getExpiryDate())));
        }
        return new CreateResult(toResponse(document, interpretation), rejected, warnings);
    }

    public Page<DocumentDtos.LibraryDocumentResponse> list(String kind, Pageable pageable) {
        String vendorId = TenantContext.vendorId();
        Page<LibraryDocument> page = kind == null
                ? repository.findByVendorId(vendorId, pageable)
                : repository.findByVendorIdAndKind(vendorId, kind, pageable);
        return page.map(d -> toResponse(refreshStatus(d), Map.of()));
    }

    public DocumentDtos.LibraryCounts counts() {
        List<LibraryDocument> all = repository.findByVendorId(TenantContext.vendorId());
        long valid = 0;
        long expiringSoon = 0;
        long expired = 0;
        for (LibraryDocument document : all) {
            switch (derivation.expiryStatus(document.getExpiryDate())) {
                case "EXPIRED" -> expired++;
                case "EXPIRING_SOON" -> expiringSoon++;
                default -> valid++;
            }
        }
        return new DocumentDtos.LibraryCounts(valid, expiringSoon, expired);
    }

    public DocumentDtos.LibraryDocumentResponse get(String libraryDocumentId) {
        return toResponse(refreshStatus(require(libraryDocumentId)), Map.of());
    }

    public LibraryDocument require(String libraryDocumentId) {
        return repository.findByIdAndVendorId(libraryDocumentId, TenantContext.vendorId())
                .orElseThrow(() -> ApiException.notFound("Library document " + libraryDocumentId));
    }

    public CreateResult update(String libraryDocumentId, DocumentDtos.LibraryDocumentRequest request) {
        LibraryDocument existing = require(libraryDocumentId);
        CreateResult created = create(request);
        repository.delete(existing);
        return created;
    }

    public void delete(String libraryDocumentId) {
        LibraryDocument document = require(libraryDocumentId);
        List<DocumentLink> links = linkRepository.findByLibraryDocumentId(libraryDocumentId);
        if (!links.isEmpty()) {
            throw new ApiException(ErrorCode.DOCUMENT_IN_USE,
                    "%s is linked to %d listing(s).".formatted(libraryDocumentId, links.size()))
                    .with("referencedBy", links.stream()
                            .map(l -> Map.of("listingId", (Object) l.getListingId())).toList());
        }
        repository.delete(document);
    }

    /** Status is recomputed on read as well as on write - server time moves, stored rows do not. */
    private LibraryDocument refreshStatus(LibraryDocument document) {
        String status = derivation.expiryStatus(document.getExpiryDate());
        if (!status.equals(document.getStatus())) {
            document.setStatus(status);
            document.setStatusComputedAt(Instant.now());
            repository.save(document);
        }
        return document;
    }

    private LocalDate normalize(Object raw, String key, Map<String, String> interpretation) {
        DateNormalizer.Normalized normalized = dateNormalizer.normalize(raw);
        if (normalized == null) {
            if (raw != null && !String.valueOf(raw).isBlank()) {
                throw new ApiException(ErrorCode.UNPARSEABLE_DATE,
                        "Accepted formats: %s.".formatted(String.join(", ", DateNormalizer.ACCEPTED_FORMATS)))
                        .with("field", key)
                        .with("rejectedValue", raw);
            }
            return null;
        }
        interpretation.put(key, normalized.pattern());
        return normalized.value();
    }

    DocumentDtos.LibraryDocumentResponse toResponse(LibraryDocument document,
                                                    Map<String, String> interpretation) {
        Long daysExpired = null;
        if (document.getExpiryDate() != null && document.getExpiryDate().isBefore(LocalDate.now())) {
            daysExpired = ChronoUnit.DAYS.between(document.getExpiryDate(), LocalDate.now());
        }
        return new DocumentDtos.LibraryDocumentResponse(
                document.getId(), document.getKind(), document.getCode(), document.getName(),
                document.getIssuingBody(), document.getReferenceNo(), document.getScope(),
                document.getIssueDate(), document.getExpiryDate(), document.getApplicability(),
                document.getDocumentId(), document.getStatus(), daysExpired,
                document.getStatusComputedAt(), interpretation.isEmpty() ? null : interpretation);
    }
}
