package com.beetloop.vendorproducts.service;

import com.beetloop.vendorproducts.catalogue.CatalogueIdService;
import com.beetloop.vendorproducts.catalogue.CatalogueService;
import com.beetloop.vendorproducts.catalogue.LockedBaselineGuard;
import com.beetloop.vendorproducts.catalogue.domain.CommercialMaster;
import com.beetloop.vendorproducts.catalogue.domain.ScientificMaster;
import com.beetloop.vendorproducts.domain.ProductCategory;
import com.beetloop.vendorproducts.domain.ProductStatus;
import com.beetloop.vendorproducts.domain.ProductVariant;
import com.beetloop.vendorproducts.domain.VendorProduct;
import com.beetloop.vendorproducts.dto.CreateProductRequest;
import com.beetloop.vendorproducts.dto.IdentityStepRequest;
import com.beetloop.vendorproducts.dto.OverallSaveRequest;
import com.beetloop.vendorproducts.dto.PageResponse;
import com.beetloop.vendorproducts.dto.ProductResponse;
import com.beetloop.vendorproducts.dto.ProductSummaryResponse;
import com.beetloop.vendorproducts.dto.QcDecisionRequest;
import com.beetloop.vendorproducts.dto.RoleStepRequest;
import com.beetloop.vendorproducts.dto.VariantRequest;
import com.beetloop.vendorproducts.dto.VariantResponse;
import com.beetloop.vendorproducts.exception.DuplicateListingException;
import com.beetloop.vendorproducts.exception.InvalidStateTransitionException;
import com.beetloop.vendorproducts.exception.ResourceNotFoundException;
import com.beetloop.vendorproducts.exception.ValidationException;
import com.beetloop.vendorproducts.repository.VendorProductRepository;
import com.beetloop.vendorproducts.security.CurrentUser;
import org.springframework.data.domain.Page;
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

/**
 * Wizard orchestration: draft creation, the three step-based saves, the
 * transactional overall save, Submit for QC and the QC decisions.
 */
@Service
public class VendorProductService {

    private static final List<ProductStatus> QC_QUEUE_STATUSES =
            List.of(ProductStatus.SUBMITTED_FOR_QC, ProductStatus.PENDING_REVIEW);
    private static final List<ProductStatus> ALL_STATUSES = List.of(ProductStatus.values());

    private final VendorProductRepository productRepository;
    private final ProductValidationService validationService;
    private final ProductMapper mapper;
    private final CatalogueService catalogue;
    private final CatalogueIdService catalogueIds;
    private final LockedBaselineGuard lockedBaselineGuard;
    private final CurrentUser currentUser;

    public VendorProductService(VendorProductRepository productRepository,
                                ProductValidationService validationService,
                                ProductMapper mapper,
                                CatalogueService catalogue,
                                CatalogueIdService catalogueIds,
                                LockedBaselineGuard lockedBaselineGuard,
                                CurrentUser currentUser) {
        this.productRepository = productRepository;
        this.validationService = validationService;
        this.mapper = mapper;
        this.catalogue = catalogue;
        this.catalogueIds = catalogueIds;
        this.lockedBaselineGuard = lockedBaselineGuard;
        this.currentUser = currentUser;
    }

    // ------------------------------------------------------------------ create

    @Transactional
    public ProductResponse create(CreateProductRequest request, String vendorId, String userId) {
        CommercialMaster t2 = catalogue.findCommercial(firstNonBlank(
                request.commercialMasterId(), request.commercialMasterCode(), request.sourceMasterId()));
        ScientificMaster t1 = catalogue.findScientific(firstNonBlank(
                request.scientificMasterId(), request.scientificMasterCode()));

        if (t2 == null && t1 != null && !t1.getStatus().isLive()) {
            ProductCategory category = request.category() != null
                    ? request.category() : ProductCategory.from(t1.getCategory());
            VendorProduct pending = new VendorProduct();
            pending.setCategory(category);
            pending.setIdentityType(request.identityType());
            pending.setName(request.name() != null ? request.name() : t1.getName());
            pending.setVendorId(vendorId);
            pending.setCreatedBy(userId);
            pending.setHoldPublish(Boolean.TRUE.equals(request.holdPublish()));
            pending.setStatus(ProductStatus.PENDING_SCIENTIFIC_MASTER);
            pending.setSourceMasterId(t1.getCode());
            return mapper.toResponse(productRepository.save(pending));
        }
        if (t2 == null && t1 != null) {
            t2 = catalogue.createPendingCommercial(t1, request);
        }

        ProductCategory category = request.category();
        if (category == null && t2 != null) {
            category = ProductCategory.from(t2.getCategory());
        }
        if (category == null) {
            throw new ValidationException("Category is required",
                    List.of(new com.beetloop.vendorproducts.dto.ApiError.FieldError(
                            "category", "Category is required unless a commercial master is provided", null)));
        }

        ProductStatus status = ProductStatus.DRAFT;
        if (t2 != null) {
            rejectDuplicate(vendorId, t2.getId());
            boolean live = t2.getStatus().isLive()
                    && t2.getScientificMaster() != null
                    && t2.getScientificMaster().getStatus().isLive();
            if (!live) {
                status = ProductStatus.PENDING_COMMERCIAL_MASTER;
            }
        } else {
            status = ProductStatus.PENDING_COMMERCIAL_MASTER;
        }

        VendorProduct product = new VendorProduct();
        product.setCategory(category);
        product.setIdentityType(identityTypeOf(request, t2));
        product.setName(request.name() != null ? request.name() : (t2 == null ? null : t2.getName()));
        product.setVendorId(vendorId);
        product.setCreatedBy(userId);
        product.setHoldPublish(Boolean.TRUE.equals(request.holdPublish()));
        product.setStatus(status);
        attachCommercial(product, t2, vendorId);
        return mapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse repointCommercial(UUID id, String commercialMasterId) {
        VendorProduct product = loadEditable(id);
        CommercialMaster t2 = catalogue.findCommercial(commercialMasterId);
        if (t2 == null) {
            throw new ResourceNotFoundException("Commercial master " + commercialMasterId + " not found");
        }
        rejectDuplicateExcept(product.getVendorId(), t2.getId(), product.getId());
        attachCommercial(product, t2, product.getVendorId());
        if (t2.getStatus().isLive() && t2.getScientificMaster().getStatus().isLive()
                && product.getStatus() == ProductStatus.PENDING_COMMERCIAL_MASTER) {
            product.setStatus(ProductStatus.DRAFT);
        }
        return mapper.toResponse(productRepository.save(product));
    }

    // ------------------------------------------------------------------- reads

    @Transactional(readOnly = true)
    public ProductResponse get(UUID id) {
        return mapper.toResponse(load(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> list(String vendorId,
                                                     ProductCategory category,
                                                     ProductStatus status,
                                                     String search,
                                                     int page,
                                                     int size,
                                                     String sort) {
        Page<VendorProduct> result = productRepository.search(
                blankToNull(vendorId), category, status, ALL_STATUSES,
                search == null ? "" : search,
                PageRequest.of(Math.max(0, page), size < 1 ? 10 : size, parseSort(sort)));
        return PageResponse.of(result, mapper::toSummary);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> qcQueue(String search, int page, int size, String sort) {
        Page<VendorProduct> result = productRepository.search(
                null, null, null, QC_QUEUE_STATUSES, search == null ? "" : search,
                PageRequest.of(Math.max(0, page), size < 1 ? 10 : size, parseSort(sort)));
        return PageResponse.of(result, mapper::toSummary);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> listPublished(ProductCategory category,
                                                             String search,
                                                             int page,
                                                             int size,
                                                             String sort) {
        Page<VendorProduct> result = productRepository.search(
                null, category, ProductStatus.PUBLISHED, List.of(ProductStatus.PUBLISHED),
                search == null ? "" : search,
                PageRequest.of(Math.max(0, page), size < 1 ? 10 : size, parseSort(sort)));
        return PageResponse.of(result, mapper::toSummary);
    }

    @Transactional(readOnly = true)
    public ProductResponse getPublished(UUID id) {
        VendorProduct product = productRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.product(id));
        if (product.getStatus() != ProductStatus.PUBLISHED) {
            throw ResourceNotFoundException.product(id);
        }
        return mapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public List<VariantResponse> listVariants(UUID productId) {
        return load(productId).getVariants().stream().map(mapper::toVariantResponse).toList();
    }

    // --------------------------------------------------- step-based (A) saves

    /** Step 1 — Product / Machine Identity. */
    @Transactional
    public ProductResponse saveIdentity(UUID id, IdentityStepRequest request) {
        VendorProduct product = loadEditable(id);

        String identityType = request.identityType() != null
                ? request.identityType()
                : product.getIdentityType();

        validationService.validateIdentity(product.getCategory(), identityType,
                request.dataOrEmpty(), request.isDraft());
        enforceLockedBaseline(product, request.dataOrEmpty());

        product.setIdentityType(identityType);
        product.setIdentityPayload(new LinkedHashMap<>(request.dataOrEmpty()));
        applyListingSummary(product);

        if (product.getStatus() == ProductStatus.DRAFT) {
            product.setStatus(ProductStatus.IDENTITY_SAVED);
        }
        return mapper.toResponse(productRepository.save(product));
    }

    /** Step 2 — Your Role &amp; Supply Information. */
    @Transactional
    public ProductResponse saveRole(UUID id, RoleStepRequest request) {
        VendorProduct product = loadEditable(id);

        validationService.validateRole(product.getCategory(), request.roleId(),
                request.dataOrEmpty(), request.isDraft());

        product.setRoleId(request.roleId());
        product.setRolePayload(new LinkedHashMap<>(request.dataOrEmpty()));

        if (product.getStatus() == ProductStatus.DRAFT || product.getStatus() == ProductStatus.IDENTITY_SAVED) {
            product.setStatus(ProductStatus.ROLE_SAVED);
        }
        return mapper.toResponse(productRepository.save(product));
    }

    /** Step 3 — add one variant. */
    @Transactional
    public VariantResponse addVariant(UUID productId, VariantRequest request) {
        VendorProduct product = loadEditable(productId);
        validationService.validateVariant(product.getCategory(), request, -1, request.isDraft());

        ProductVariant variant = new ProductVariant();
        product.addVariant(variant);
        mapper.applyVariant(variant, request);

        advanceToVariantsSaved(product);
        productRepository.save(product);
        return mapper.toVariantResponse(variant);
    }

    /** Step 3 — update one variant (all sections present in the payload). */
    @Transactional
    public VariantResponse updateVariant(UUID productId, UUID variantId, VariantRequest request) {
        VendorProduct product = loadEditable(productId);
        validationService.validateVariant(product.getCategory(), request, -1, request.isDraft());

        ProductVariant variant = findVariant(product, variantId);
        mapper.applyVariant(variant, request);

        advanceToVariantsSaved(product);
        productRepository.save(product);
        return mapper.toVariantResponse(variant);
    }

    /**
     * Step 3 — save one variant sub-step independently, so the vendor can leave
     * the Add Variant wizard mid-way without losing what was already entered.
     */
    @Transactional
    public VariantResponse saveVariantSection(UUID productId, UUID variantId, String section,
                                              VariantRequest request) {
        VendorProduct product = loadEditable(productId);
        ProductVariant variant = findVariant(product, variantId);

        switch (section) {
            case "details" -> {
                validationService.validateVariant(product.getCategory(),
                        new VariantRequest(request.variantDetails(), null, null, null, null, request.draft()),
                        -1, request.isDraft());
                mapper.applyVariantDetails(variant, request.variantDetails());
            }
            case "technical-specifications" -> {
                validationService.validateVariant(product.getCategory(),
                        new VariantRequest(null, request.technicalSpecifications(), null, null, null,
                                request.draft()),
                        -1, request.isDraft());
                mapper.applyTechnicalSpecifications(variant, request.technicalSpecifications());
            }
            case "commercial-pricing" -> mapper.applyCommercialPricing(variant, request.commercialPricing());
            case "compliance-documents" -> {
                validationService.validateVariant(product.getCategory(),
                        new VariantRequest(null, null, null, request.complianceCertifications(), null,
                                request.draft()),
                        -1, request.isDraft());
                mapper.applyComplianceDocuments(variant, request.complianceCertifications());
            }
            case "search-marketplace" -> mapper.applySearchMarketplace(variant, request.searchMarketplace());
            default -> throw new IllegalArgumentException("Unknown variant section '" + section + "'. "
                    + "Expected details, technical-specifications, commercial-pricing, "
                    + "compliance-documents or search-marketplace");
        }

        advanceToVariantsSaved(product);
        productRepository.save(product);
        return mapper.toVariantResponse(variant);
    }

    @Transactional
    public void deleteVariant(UUID productId, UUID variantId) {
        VendorProduct product = loadEditable(productId);
        ProductVariant variant = findVariant(product, variantId);
        product.removeVariant(variant);
        int index = 0;
        for (ProductVariant remaining : product.getVariants()) {
            remaining.setPosition(index++);
        }
        productRepository.save(product);
    }

    // ------------------------------------------------------- overall (B) save

    /**
     * Overall save. Runs in one transaction: either every section lands or none
     * does, so a partially-applied product cannot be left behind.
     */
    @Transactional
    public ProductResponse saveAll(UUID id, OverallSaveRequest request) {
        VendorProduct product = loadEditable(id);
        boolean draft = request.isDraft();

        if (request.productIdentity() != null) {
            IdentityStepRequest identity = request.productIdentity();
            String identityType = identity.identityType() != null
                    ? identity.identityType()
                    : product.getIdentityType();
            validationService.validateIdentity(product.getCategory(), identityType,
                    identity.dataOrEmpty(), draft || identity.isDraft());
            enforceLockedBaseline(product, identity.dataOrEmpty());
            product.setIdentityType(identityType);
            product.setIdentityPayload(new LinkedHashMap<>(identity.dataOrEmpty()));
        }

        if (request.yourRole() != null) {
            RoleStepRequest role = request.yourRole();
            validationService.validateRole(product.getCategory(), role.roleId(),
                    role.dataOrEmpty(), draft || role.isDraft());
            product.setRoleId(role.roleId());
            product.setRolePayload(new LinkedHashMap<>(role.dataOrEmpty()));
        }

        if (request.variantsOrNull() != null) {
            List<VariantRequest> incoming = request.variantsOrEmpty();
            for (int i = 0; i < incoming.size(); i++) {
                validationService.validateVariant(product.getCategory(), incoming.get(i), i, draft);
            }
            product.getVariants().clear();
            for (VariantRequest variantRequest : incoming) {
                ProductVariant variant = new ProductVariant();
                product.addVariant(variant);
                mapper.applyVariant(variant, variantRequest);
            }
        }

        applyListingSummary(product);

        if (!draft) {
            validationService.validateCompleteProduct(product);
            if (!product.getStatus().isSubmitted()) {
                product.setStatus(ProductStatus.VARIANTS_SAVED);
            }
        } else {
            advanceDraftStatus(product);
        }

        if (request.isSubmitForQc()) {
            requireLiveCommercialForSubmit(product);
            validationService.validateCompleteProduct(product);
            transitionToSubmitted(product);
        }

        productRepository.save(product);
        return mapper.toResponse(product);
    }

    // ------------------------------------------------------------ QC workflow

    @Transactional
    public ProductResponse submitForQc(UUID id) {
        VendorProduct product = load(id);
        if (product.getStatus() == ProductStatus.SUBMITTED_FOR_QC
                || product.getStatus() == ProductStatus.PENDING_REVIEW) {
            throw InvalidStateTransitionException.cannotSubmit(product.getStatus());
        }
        if (product.getStatus() == ProductStatus.APPROVED || product.getStatus() == ProductStatus.PUBLISHED
                || product.getStatus() == ProductStatus.AWAITING_CATALOGUE_APPROVAL
                || product.getStatus() == ProductStatus.SUSPENDED) {
            throw InvalidStateTransitionException.cannotSubmit(product.getStatus());
        }
        requireLiveCommercialForSubmit(product);
        validationService.validateCompleteProduct(product);
        transitionToSubmitted(product);
        return mapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse qcDecision(UUID id, QcDecisionRequest request) {
        VendorProduct product = load(id);
        if (!QC_QUEUE_STATUSES.contains(product.getStatus())) {
            throw InvalidStateTransitionException.cannotReview(product.getStatus());
        }

        String decision = request.decision().toUpperCase(Locale.ROOT);
        if (("REJECT".equals(decision) || "QUERY".equals(decision))
                && (request.remarks() == null || request.remarks().isBlank())) {
            throw new ValidationException("QC decision is incomplete",
                    List.of(new com.beetloop.vendorproducts.dto.ApiError.FieldError(
                            "remarks", "Remarks are required when rejecting or raising a query", null)));
        }

        switch (decision) {
            case "APPROVE" -> applyVendorApprove(product);
            case "PUBLISH" -> {
                requireGoLive(product);
                product.setStatus(ProductStatus.PUBLISHED);
                product.setVerified(true);
            }
            case "REJECT" -> product.setStatus(ProductStatus.REJECTED);
            case "QUERY" -> product.setStatus(ProductStatus.QUERY);
            default -> throw new IllegalArgumentException("Unknown decision " + decision);
        }

        product.setQcReviewer(blankToNull(request.reviewer()) != null
                ? request.reviewer() : currentUser.userId());
        product.setQcRemarks(request.remarks());
        product.setReviewedAt(Instant.now());
        return mapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(UUID id) {
        productRepository.delete(load(id));
    }

    // ------------------------------------------------------------- internals

    private VendorProduct load(UUID id) {
        VendorProduct product = productRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.product(id));
        currentUser.requireOwner(product.getVendorId());
        return product;
    }

    /**
     * Loads a product that may still be edited. A product returned by QC
     * (REJECTED / QUERY) is editable again; an approved or published one is not.
     */
    private VendorProduct loadEditable(UUID id) {
        VendorProduct product = load(id);
        ProductStatus status = product.getStatus();
        if (status == ProductStatus.APPROVED || status == ProductStatus.PUBLISHED
                || status == ProductStatus.SUBMITTED_FOR_QC || status == ProductStatus.PENDING_REVIEW
                || status == ProductStatus.AWAITING_CATALOGUE_APPROVAL
                || status == ProductStatus.SUSPENDED) {
            throw InvalidStateTransitionException.notEditable(status);
        }
        return product;
    }

    private ProductVariant findVariant(VendorProduct product, UUID variantId) {
        return product.getVariants().stream()
                .filter(v -> variantId.equals(v.getId()))
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.variant(variantId));
    }

    private void transitionToSubmitted(VendorProduct product) {
        product.setStatus(ProductStatus.SUBMITTED_FOR_QC);
        product.setSubmittedAt(Instant.now());
        product.setReviewedAt(null);
        if (product.getSku() == null || product.getSku().isBlank()) {
            product.setSku(generateSku(product));
        }
    }

    private void advanceToVariantsSaved(VendorProduct product) {
        if (product.getStatus() == ProductStatus.DRAFT
                || product.getStatus() == ProductStatus.IDENTITY_SAVED
                || product.getStatus() == ProductStatus.ROLE_SAVED) {
            product.setStatus(ProductStatus.VARIANTS_SAVED);
        }
    }

    private void advanceDraftStatus(VendorProduct product) {
        if (product.getStatus().isSubmitted()) {
            return;
        }
        if (!product.getVariants().isEmpty()) {
            product.setStatus(ProductStatus.VARIANTS_SAVED);
        } else if (product.getRoleId() != null && !product.getRoleId().isBlank()) {
            product.setStatus(ProductStatus.ROLE_SAVED);
        } else if (product.getIdentityPayload() != null && !product.getIdentityPayload().isEmpty()) {
            product.setStatus(ProductStatus.IDENTITY_SAVED);
        }
    }

    /**
     * Derives the catalog-card columns from whichever identity fields the chosen
     * category actually uses, so {@code GET /products} can render a row without
     * unpacking the JSON payload.
     */
    private void applyListingSummary(VendorProduct product) {
        Map<String, Object> identity = product.getIdentityPayload();
        if (identity == null || identity.isEmpty()) {
            return;
        }

        String name = firstNonBlank(identity,
                "commercialProductName", "productName", "machineName", "commercialName",
                "extractName", "commodityName", "ingredientName", "commonName", "compoundName", "blendName");
        if (name != null) {
            product.setName(name);
        }

        String listingCategory = firstNonBlank(identity,
                "category", "packagingCategory", "functionalClass", "regulatoryCategory",
                "vitaminClass", "additiveType", "sweetenerType", "enzymeType", "extractType");
        if (listingCategory != null) {
            product.setListingCategory(listingCategory);
        }

        String origin = firstNonBlank(identity,
                "countryOfOrigin", "countryOfManufacture", "cropOriginCountry",
                "originCountry", "originOfManufacture", "originSource", "sourceCountry", "inputOrigin");
        if (origin != null) {
            product.setOriginCountry(origin);
        }

        if (product.getThumbEmoji() == null || product.getThumbEmoji().isBlank()) {
            product.setThumbEmoji(defaultEmoji(product.getCategory()));
        }
    }

    private String defaultEmoji(ProductCategory category) {
        return switch (category) {
            case RAW_MATERIALS -> "🧪";
            case PROCESSING_MACHINERY -> "🏭";
            case FINISHED_GOODS -> "🧃";
            case PACKAGING_MATERIALS -> "📦";
            case PACKAGING_MACHINERY -> "⚙️";
        };
    }

    private String firstNonBlank(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value instanceof String s && !s.isBlank()) {
                return s;
            }
        }
        return null;
    }

    /** Mirrors the frontend's vendor-SKU rule: first 3 letters of the first 2 words + "-001". */
    private String generateSku(VendorProduct product) {
        String base = product.getName() == null ? "" : product.getName();
        String[] words = base.trim().split("\\s+");
        StringBuilder prefix = new StringBuilder();
        int used = 0;
        for (String word : words) {
            String cleaned = word.replaceAll("[^a-zA-Z0-9]", "");
            if (cleaned.isEmpty()) {
                continue;
            }
            if (used > 0) {
                prefix.append('-');
            }
            prefix.append(cleaned.substring(0, Math.min(3, cleaned.length())).toUpperCase(Locale.ROOT));
            if (++used == 2) {
                break;
            }
        }
        return (prefix.isEmpty() ? "PRD" : prefix.toString()) + "-001";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private void enforceLockedBaseline(VendorProduct product, Map<String, Object> incoming) {
        CommercialMaster t2 = attachedCommercial(product);
        if (t2 == null) {
            return;
        }
        if (lockedBaselineGuard.isGradeDefiningChange(t2, incoming)) {
            throw new InvalidStateTransitionException(
                    "BRANCH_REQUIRED — grade-defining change must branch to a new T2");
        }
        lockedBaselineGuard.rejectIfBaselineMutated(t2, incoming);
    }

    private void requireLiveCommercialForSubmit(VendorProduct product) {
        CommercialMaster t2 = attachedCommercial(product);
        if (t2 == null || !t2.getStatus().isLive()
                || t2.getScientificMaster() == null || !t2.getScientificMaster().getStatus().isLive()) {
            throw new InvalidStateTransitionException("PENDING COMMERCIAL MASTER");
        }
    }

    private void applyVendorApprove(VendorProduct product) {
        product.setVerified(true);
        if (canGoLive(product)) {
            product.setStatus(ProductStatus.PUBLISHED);
        } else {
            product.setStatus(ProductStatus.AWAITING_CATALOGUE_APPROVAL);
        }
    }

    private void requireGoLive(VendorProduct product) {
        if (!canGoLive(product)) {
            throw new InvalidStateTransitionException(
                    "Cannot publish: awaiting catalogue approval (T1 and T2 must be live, docs valid, no hold)");
        }
    }

    private boolean canGoLive(VendorProduct product) {
        if (product.isHoldPublish()) {
            return false;
        }
        CommercialMaster t2 = attachedCommercial(product);
        if (t2 == null || !t2.getStatus().isLive()
                || t2.getScientificMaster() == null || !t2.getScientificMaster().getStatus().isLive()) {
            return false;
        }
        try {
            validationService.validateCompleteProduct(product);
            return true;
        } catch (ValidationException ex) {
            return false;
        }
    }

    private CommercialMaster attachedCommercial(VendorProduct product) {
        if (product.getCommercialMasterId() == null) {
            return null;
        }
        return catalogue.findCommercial(product.getCommercialMasterId().toString());
    }

    private void attachCommercial(VendorProduct product, CommercialMaster t2, String vendorId) {
        if (t2 == null) {
            return;
        }
        product.setCommercialMasterId(t2.getId());
        product.setSourceMasterId(t2.getCode());
        product.setListingCode(catalogueIds.listingCode(t2.getCode(), vendorId));
    }

    private void rejectDuplicate(String vendorId, UUID commercialMasterId) {
        rejectDuplicateExcept(vendorId, commercialMasterId, null);
    }

    private void rejectDuplicateExcept(String vendorId, UUID commercialMasterId, UUID exceptId) {
        if (vendorId == null || commercialMasterId == null) {
            return;
        }
        for (VendorProduct existing : productRepository.findActiveByVendorAndCommercialMaster(
                vendorId, commercialMasterId, ProductStatus.REJECTED)) {
            if (exceptId != null && exceptId.equals(existing.getId())) {
                continue;
            }
            throw new DuplicateListingException(existing.getId(), existing.getListingCode());
        }
    }

    private String identityTypeOf(CreateProductRequest request, CommercialMaster t2) {
        if (request.identityType() != null && !request.identityType().isBlank()) {
            return request.identityType();
        }
        if (t2 != null && t2.getBaseline() != null) {
            Object value = t2.getBaseline().get("identityType");
            if (value instanceof String s && !s.isBlank()) {
                return s;
            }
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "updatedAt");
        }
        String[] parts = sort.split(",");
        String property = parts[0].trim();
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }
}
