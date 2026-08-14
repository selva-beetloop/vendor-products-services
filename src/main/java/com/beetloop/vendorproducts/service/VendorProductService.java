package com.beetloop.vendorproducts.service;

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
import com.beetloop.vendorproducts.exception.InvalidStateTransitionException;
import com.beetloop.vendorproducts.exception.ResourceNotFoundException;
import com.beetloop.vendorproducts.exception.ValidationException;
import com.beetloop.vendorproducts.repository.VendorProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

    private final VendorProductRepository productRepository;
    private final ProductValidationService validationService;
    private final ProductMapper mapper;

    /**
     * Used instead of {@code repository.save()} on the variant paths.
     * {@code save()} on an entity that already has an id performs an
     * {@code EntityManager.merge()}, and merging a graph that contains a
     * <em>transient</em> child returns a managed <em>copy</em> — the child
     * instance we still hold a reference to never receives its generated id.
     * Since the product is already managed inside these transactions, an explicit
     * flush is both sufficient and correct: it cascades the persist and populates
     * the ids on the instances we are about to map into the response.
     */
    @PersistenceContext
    private EntityManager entityManager;

    public VendorProductService(VendorProductRepository productRepository,
                                ProductValidationService validationService,
                                ProductMapper mapper) {
        this.productRepository = productRepository;
        this.validationService = validationService;
        this.mapper = mapper;
    }

    // ------------------------------------------------------------------ create

    @Transactional
    public ProductResponse create(CreateProductRequest request, String vendorId, String userId) {
        VendorProduct product = new VendorProduct();
        product.setCategory(request.category());
        product.setIdentityType(request.identityType());
        product.setSourceMasterId(request.sourceMasterId());
        product.setName(request.name());
        product.setVendorId(vendorId);
        product.setCreatedBy(userId);
        product.setStatus(ProductStatus.DRAFT);
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
                blankToNull(vendorId), category, status, null, blankToNull(search),
                PageRequest.of(Math.max(0, page), size < 1 ? 10 : size, parseSort(sort)));
        return PageResponse.of(result, mapper::toSummary);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> qcQueue(String search, int page, int size, String sort) {
        Page<VendorProduct> result = productRepository.search(
                null, null, null, QC_QUEUE_STATUSES, blankToNull(search),
                PageRequest.of(Math.max(0, page), size < 1 ? 10 : size, parseSort(sort)));
        return PageResponse.of(result, mapper::toSummary);
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
        entityManager.flush();
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
        entityManager.flush();
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
        entityManager.flush();
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
            // Flush the orphan-removal of the old variants before adding the new
            // ones; clearing and re-populating a collection in a single flush
            // makes Hibernate re-insert rows it is also deleting.
            product.getVariants().clear();
            entityManager.flush();
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
            validationService.validateCompleteProduct(product);
            transitionToSubmitted(product);
        }

        // Flush rather than save(): the product is managed, and newly added
        // variants need their generated ids before the response is mapped.
        entityManager.flush();
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
        if (product.getStatus() == ProductStatus.APPROVED || product.getStatus() == ProductStatus.PUBLISHED) {
            throw InvalidStateTransitionException.cannotSubmit(product.getStatus());
        }
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
            case "APPROVE" -> {
                product.setStatus(ProductStatus.APPROVED);
                product.setVerified(true);
            }
            case "PUBLISH" -> {
                product.setStatus(ProductStatus.PUBLISHED);
                product.setVerified(true);
            }
            case "REJECT" -> product.setStatus(ProductStatus.REJECTED);
            case "QUERY" -> product.setStatus(ProductStatus.QUERY);
            default -> throw new IllegalArgumentException("Unknown decision " + decision);
        }

        product.setQcReviewer(request.reviewer());
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
        return productRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.product(id));
    }

    /**
     * Loads a product that may still be edited. A product returned by QC
     * (REJECTED / QUERY) is editable again; an approved or published one is not.
     */
    private VendorProduct loadEditable(UUID id) {
        VendorProduct product = load(id);
        ProductStatus status = product.getStatus();
        if (status == ProductStatus.APPROVED || status == ProductStatus.PUBLISHED
                || status == ProductStatus.SUBMITTED_FOR_QC || status == ProductStatus.PENDING_REVIEW) {
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
