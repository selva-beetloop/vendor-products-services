package com.beetloop.vendorproducts.catalogue;

import com.beetloop.vendorproducts.catalogue.domain.CatalogueKind;
import com.beetloop.vendorproducts.catalogue.domain.CatalogueStatus;
import com.beetloop.vendorproducts.catalogue.domain.CommercialMaster;
import com.beetloop.vendorproducts.catalogue.domain.ScientificMaster;
import com.beetloop.vendorproducts.catalogue.dto.CatalogueDtos;
import com.beetloop.vendorproducts.catalogue.repository.CommercialMasterRepository;
import com.beetloop.vendorproducts.catalogue.repository.ScientificMasterRepository;
import com.beetloop.vendorproducts.domain.ProductStatus;
import com.beetloop.vendorproducts.domain.VendorProduct;
import com.beetloop.vendorproducts.dto.CreateProductRequest;
import com.beetloop.vendorproducts.dto.PageResponse;
import com.beetloop.vendorproducts.exception.InvalidStateTransitionException;
import com.beetloop.vendorproducts.exception.ResourceNotFoundException;
import com.beetloop.vendorproducts.exception.ValidationException;
import com.beetloop.vendorproducts.repository.VendorProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class CatalogueService {

    private static final List<CatalogueStatus> LIVE = List.of(CatalogueStatus.LIVE, CatalogueStatus.APPROVED);
    private static final List<CatalogueStatus> INTEL_QUEUE = List.of(CatalogueStatus.SUBMITTED, CatalogueStatus.IN_QC);

    private static final List<CatalogueStatus> ALL_STATUSES = List.of(CatalogueStatus.values());

    private final ScientificMasterRepository scientificMasters;
    private final CommercialMasterRepository commercialMasters;
    private final CatalogueIdService ids;
    private final VendorProductRepository products;

    public CatalogueService(ScientificMasterRepository scientificMasters,
                            CommercialMasterRepository commercialMasters,
                            CatalogueIdService ids,
                            VendorProductRepository products) {
        this.scientificMasters = scientificMasters;
        this.commercialMasters = commercialMasters;
        this.ids = ids;
        this.products = products;
    }

    @Transactional(readOnly = true)
    public PageResponse<CatalogueDtos.CommercialMasterResponse> searchLiveCommercial(
            String search, String category, CatalogueKind kind, int page, int size) {
        return searchCommercial(search, category, kind, page, size, true);
    }

    @Transactional(readOnly = true)
    public PageResponse<CatalogueDtos.CommercialMasterResponse> searchCommercial(
            String search, String category, CatalogueKind kind, int page, int size, boolean liveOnly) {
        return PageResponse.of(
                commercialMasters.search(kind, blankToNull(category), liveOnly ? LIVE : ALL_STATUSES,
                        search == null ? "" : search,
                        PageRequest.of(Math.max(0, page), size < 1 ? 20 : size, Sort.by("name"))),
                this::toCommercial);
    }

    @Transactional(readOnly = true)
    public PageResponse<CatalogueDtos.ScientificMasterResponse> searchLiveScientific(
            String search, String category, CatalogueKind kind, int page, int size) {
        return searchScientific(search, category, kind, page, size, true);
    }

    @Transactional(readOnly = true)
    public PageResponse<CatalogueDtos.ScientificMasterResponse> searchScientific(
            String search, String category, CatalogueKind kind, int page, int size, boolean liveOnly) {
        return PageResponse.of(
                scientificMasters.search(kind, blankToNull(category), liveOnly ? LIVE : ALL_STATUSES,
                        search == null ? "" : search,
                        PageRequest.of(Math.max(0, page), size < 1 ? 20 : size, Sort.by("name"))),
                this::toScientific);
    }

    @Transactional(readOnly = true)
    public CommercialMaster requireLiveCommercial(UUID id) {
        CommercialMaster master = commercialMasters.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commercial master " + id + " not found"));
        if (!master.getStatus().isLive()) {
            throw new InvalidStateTransitionException("Commercial master " + master.getCode()
                    + " is not live (status " + master.getStatus() + ")");
        }
        if (!master.getScientificMaster().getStatus().isLive()) {
            throw new InvalidStateTransitionException(
                    "PENDING COMMERCIAL MASTER — scientific parent " + master.getScientificMaster().getCode()
                            + " is not live");
        }
        return master;
    }

    @Transactional(readOnly = true)
    public CommercialMaster findCommercial(String idOrCode) {
        if (idOrCode == null || idOrCode.isBlank()) {
            return null;
        }
        String raw = idOrCode.trim();
        try {
            return commercialMasters.findById(UUID.fromString(raw)).orElse(null);
        } catch (IllegalArgumentException ignored) {
            return commercialMasters.findByCode(raw.toUpperCase(Locale.ROOT)).orElse(null);
        }
    }

    @Transactional(readOnly = true)
    public ScientificMaster findScientific(String idOrCode) {
        if (idOrCode == null || idOrCode.isBlank()) {
            return null;
        }
        String raw = idOrCode.trim();
        try {
            return scientificMasters.findById(UUID.fromString(raw)).orElse(null);
        } catch (IllegalArgumentException ignored) {
            return scientificMasters.findByCode(raw.toUpperCase(Locale.ROOT)).orElse(null);
        }
    }

    @Transactional
    public CatalogueDtos.ScientificMasterResponse createScientific(CatalogueDtos.CreateScientificRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ValidationException("Scientific master is incomplete",
                    List.of(new com.beetloop.vendorproducts.dto.ApiError.FieldError("name", "Name is required", null)));
        }
        ScientificMaster t1 = new ScientificMaster();
        t1.setKind(request.kind() == null ? CatalogueKind.PRODUCT : request.kind());
        t1.setCategory(request.category());
        t1.setName(request.name());
        t1.setCasNumber(request.casNumber());
        t1.setFormula(request.formula());
        t1.setPayload(request.payload() == null ? Map.of() : new LinkedHashMap<>(request.payload()));
        t1.setStatus(CatalogueStatus.SUBMITTED);
        t1.setCode(ids.nextScientific(request.name()));
        return toScientific(scientificMasters.save(t1));
    }

    @Transactional
    public CatalogueDtos.CommercialMasterResponse createCommercial(CatalogueDtos.CreateCommercialRequest request,
                                                                  boolean submitForIntel) {
        ScientificMaster t1 = resolveScientific(request.scientificMasterId(), request.scientificMasterCode());
        if (!t1.getStatus().isLive()) {
            throw new InvalidStateTransitionException(
                    "T2 insert is blocked unless T1 is Intelligence-QC approved. "
                            + t1.getCode() + " is " + t1.getStatus());
        }
        CommercialMaster t2 = new CommercialMaster();
        t2.setScientificMaster(t1);
        t2.setKind(request.kind() == null ? t1.getKind() : request.kind());
        t2.setCategory(request.category() == null ? t1.getCategory() : request.category());
        t2.setName(request.name() == null ? t1.getName() : request.name());
        t2.setAssay(request.assay());
        t2.setGrade(request.grade());
        t2.setForm(request.form());
        t2.setOrigin(request.origin());
        t2.setColour(request.colour());
        t2.setSource(request.source());
        t2.refreshGradeKey();
        commercialMasters.findByScientificMasterAndGradeKey(t1, t2.getGradeKey()).ifPresent(existing -> {
            throw new InvalidStateTransitionException(
                    "Grade already exists as " + existing.getCode() + " — use Flow A on that T2");
        });
        t2.setBaseline(buildBaseline(t1, t2, request.baseline()));
        t2.setStatus(submitForIntel ? CatalogueStatus.SUBMITTED : CatalogueStatus.LIVE);
        t2.setCode(ids.nextCommercial(tokenFrom(t1, t2)));
        return toCommercial(commercialMasters.save(t2));
    }

    @Transactional
    public CommercialMaster createPendingCommercial(ScientificMaster t1, CreateProductRequest request) {
        String grade = first(request.grade(), first(request.name(), t1.getName()));
        String gradeKey = CommercialMaster.normalize(request.assay()) + "|"
                + CommercialMaster.normalize(grade) + "|"
                + CommercialMaster.normalize(request.form()) + "|"
                + CommercialMaster.normalize(request.origin()) + "|"
                + CommercialMaster.normalize(request.colour()) + "|"
                + CommercialMaster.normalize(request.source());
        var existing = commercialMasters.findByScientificMasterAndGradeKey(t1, gradeKey);
        if (existing.isPresent()) {
            return existing.get();
        }
        CatalogueDtos.CreateCommercialRequest create = new CatalogueDtos.CreateCommercialRequest(
                t1.getId(),
                t1.getCode(),
                t1.getKind(),
                request.category() == null ? t1.getCategory() : request.category().getId(),
                request.name() == null ? t1.getName() : request.name(),
                request.assay(),
                grade,
                request.form(),
                request.origin(),
                request.colour(),
                request.source(),
                null);
        CatalogueDtos.CommercialMasterResponse created = createCommercial(create, true);
        return commercialMasters.findByCode(created.code()).orElseThrow();
    }

    @Transactional
    public CatalogueDtos.CommercialMasterResponse branch(String parentCode, CatalogueDtos.BranchRequest request) {
        CommercialMaster parent = commercialMasters.findByCode(parentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Commercial master " + parentCode + " not found"));
        CatalogueDtos.CreateCommercialRequest create = new CatalogueDtos.CreateCommercialRequest(
                parent.getScientificMaster().getId(),
                parent.getScientificMaster().getCode(),
                parent.getKind(),
                parent.getCategory(),
                request.name() == null ? parent.getName() : request.name(),
                first(request.assay(), parent.getAssay()),
                first(request.grade(), parent.getGrade()),
                first(request.form(), parent.getForm()),
                first(request.origin(), parent.getOrigin()),
                first(request.colour(), parent.getColour()),
                first(request.source(), parent.getSource()),
                parent.getBaseline());
        String gradeKey = CommercialMaster.normalize(create.assay()) + "|" + CommercialMaster.normalize(create.grade())
                + "|" + CommercialMaster.normalize(create.form()) + "|" + CommercialMaster.normalize(create.origin())
                + "|" + CommercialMaster.normalize(create.colour()) + "|" + CommercialMaster.normalize(create.source());
        var existing = commercialMasters.findByScientificMasterAndGradeKey(parent.getScientificMaster(), gradeKey);
        if (existing.isPresent()) {
            return toCommercial(existing.get());
        }
        CatalogueDtos.CommercialMasterResponse created = createCommercial(create, true);
        CommercialMaster saved = commercialMasters.findByCode(created.code()).orElseThrow();
        saved.setParentCode(parent.getCode());
        return toCommercial(commercialMasters.save(saved));
    }

    @Transactional(readOnly = true)
    public PageResponse<CatalogueDtos.IntelQcRow> intelQueue(String search, int page, int size) {
        var pageable = PageRequest.of(Math.max(0, page), size < 1 ? 50 : size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        String q = blankToNull(search);
        var t1 = q == null
                ? scientificMasters.findByStatusIn(INTEL_QUEUE, pageable)
                : scientificMasters.search(null, null, INTEL_QUEUE, q, pageable);
        var t2 = q == null
                ? commercialMasters.findByStatusIn(INTEL_QUEUE, pageable)
                : commercialMasters.search(null, null, INTEL_QUEUE, q, pageable);
        java.util.ArrayList<CatalogueDtos.IntelQcRow> rows = new java.util.ArrayList<>();
        t1.forEach(s -> rows.add(new CatalogueDtos.IntelQcRow(
                s.getId(), s.getCode(), s.getKind().name(), "T1", s.getName(), s.getCategory(),
                s.getStatus(), s.getUpdatedAt())));
        t2.forEach(c -> rows.add(new CatalogueDtos.IntelQcRow(
                c.getId(), c.getCode(), c.getKind().name(), "T2", c.getName(), c.getCategory(),
                c.getStatus(), c.getUpdatedAt())));
        rows.sort((a, b) -> b.updatedAt().compareTo(a.updatedAt()));
        int from = Math.min(page * size, rows.size());
        int to = Math.min(from + Math.max(size, 1), rows.size());
        return new PageResponse<>(rows.subList(from, to), page, size, rows.size(),
                size < 1 ? 1 : (int) Math.ceil(rows.size() / (double) size),
                page == 0, to >= rows.size());
    }

    @Transactional
    public void intelDecision(String layer, UUID id, CatalogueDtos.IntelQcDecisionRequest request) {
        String decision = request.decision() == null ? "" : request.decision().toUpperCase(Locale.ROOT);
        if (("REJECT".equals(decision) || "QUERY".equals(decision))
                && (request.remarks() == null || request.remarks().isBlank())) {
            throw new ValidationException("QC decision is incomplete",
                    List.of(new com.beetloop.vendorproducts.dto.ApiError.FieldError(
                            "remarks", "Remarks are required when rejecting or raising a query", null)));
        }
        if ("T1".equalsIgnoreCase(layer) || "SCI".equalsIgnoreCase(layer)) {
            ScientificMaster t1 = scientificMasters.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Scientific master " + id + " not found"));
            applyIntel(t1::setStatus, t1::setQcReviewer, t1::setQcRemarks, t1::setReviewedAt, decision, request);
            scientificMasters.save(t1);
            return;
        }
        CommercialMaster t2 = commercialMasters.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commercial master " + id + " not found"));
        applyIntel(t2::setStatus, t2::setQcReviewer, t2::setQcRemarks, t2::setReviewedAt, decision, request);
        commercialMasters.save(t2);
        if (t2.getStatus().isLive()) {
            releaseVendorListings(t2);
        }
    }

    private void releaseVendorListings(CommercialMaster t2) {
        for (VendorProduct listing : products.findByCommercialMasterId(t2.getId())) {
            if (listing.getStatus() == ProductStatus.PENDING_COMMERCIAL_MASTER) {
                listing.setStatus(ProductStatus.DRAFT);
                products.save(listing);
            } else if (listing.getStatus() == ProductStatus.AWAITING_CATALOGUE_APPROVAL
                    && !listing.isHoldPublish()) {
                listing.setStatus(ProductStatus.PUBLISHED);
                listing.setVerified(true);
                products.save(listing);
            }
        }
    }

    private void applyIntel(java.util.function.Consumer<CatalogueStatus> status,
                            java.util.function.Consumer<String> reviewer,
                            java.util.function.Consumer<String> remarks,
                            java.util.function.Consumer<Instant> reviewedAt,
                            String decision,
                            CatalogueDtos.IntelQcDecisionRequest request) {
        switch (decision) {
            case "APPROVE", "PUBLISH" -> status.accept(CatalogueStatus.LIVE);
            case "REJECT" -> status.accept(CatalogueStatus.REJECTED);
            case "QUERY" -> status.accept(CatalogueStatus.QUERY);
            default -> throw new IllegalArgumentException("Unknown decision " + decision);
        }
        reviewer.accept(request.reviewer());
        remarks.accept(request.remarks());
        reviewedAt.accept(Instant.now());
    }

    public CatalogueDtos.ScientificMasterResponse toScientific(ScientificMaster s) {
        return new CatalogueDtos.ScientificMasterResponse(
                s.getId(), s.getCode(), s.getKind(), s.getCategory(), s.getName(),
                s.getCasNumber(), s.getFormula(), s.getStatus(),
                s.getPayload() == null ? Map.of() : s.getPayload(),
                s.getQcReviewer(), s.getQcRemarks(), s.getReviewedAt(),
                s.getCreatedAt(), s.getUpdatedAt());
    }

    public CatalogueDtos.CommercialMasterResponse toCommercial(CommercialMaster c) {
        ScientificMaster s = c.getScientificMaster();
        return new CatalogueDtos.CommercialMasterResponse(
                c.getId(), c.getCode(), s.getId(), s.getCode(), c.getKind(), c.getCategory(),
                c.getName(), c.getAssay(), c.getGrade(), c.getForm(), c.getOrigin(),
                c.getColour(), c.getSource(), c.getStatus(),
                c.getBaseline() == null ? Map.of() : c.getBaseline(),
                c.getParentCode(), c.getQcReviewer(), c.getQcRemarks(), c.getReviewedAt(),
                c.getCreatedAt(), c.getUpdatedAt());
    }

    private ScientificMaster resolveScientific(UUID id, String code) {
        if (id != null) {
            return scientificMasters.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Scientific master " + id + " not found"));
        }
        if (code != null) {
            return scientificMasters.findByCode(code)
                    .orElseThrow(() -> new ResourceNotFoundException("Scientific master " + code + " not found"));
        }
        throw new ValidationException("Scientific master is required",
                List.of(new com.beetloop.vendorproducts.dto.ApiError.FieldError(
                        "scientificMasterId", "T2 requires an approved T1", null)));
    }

    private Map<String, Object> buildBaseline(ScientificMaster t1, CommercialMaster t2, Map<String, Object> extra) {
        Map<String, Object> baseline = new LinkedHashMap<>();
        if (extra != null) {
            baseline.putAll(extra);
        }
        put(baseline, "botanicalName", t1.getName());
        put(baseline, "casNumber", t1.getCasNumber());
        put(baseline, "casNo", t1.getCasNumber());
        put(baseline, "formula", t1.getFormula());
        put(baseline, "assay", t2.getAssay());
        put(baseline, "assayPurity", t2.getAssay());
        put(baseline, "markerAssay", t2.getAssay());
        put(baseline, "grade", t2.getGrade());
        put(baseline, "form", t2.getForm());
        put(baseline, "physicalForm", t2.getForm());
        put(baseline, "origin", t2.getOrigin());
        put(baseline, "countryOfOrigin", t2.getOrigin());
        put(baseline, "colour", t2.getColour());
        put(baseline, "color", t2.getColour());
        put(baseline, "source", t2.getSource());
        return baseline;
    }

    private static void put(Map<String, Object> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.putIfAbsent(key, value);
        }
    }

    private static String tokenFrom(ScientificMaster t1, CommercialMaster t2) {
        String assay = t2.getAssay() == null ? "" : t2.getAssay().replaceAll("[^A-Za-z0-9]", "");
        return CatalogueIdService.token(t1.getName()) + assay;
    }

    private static String first(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
