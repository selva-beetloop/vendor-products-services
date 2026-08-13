package com.beetloop.catalog.product;

import com.beetloop.catalog.product.dto.ProductDtos;
import com.beetloop.catalog.product.model.ProductListing;
import com.beetloop.catalog.product.model.ProductStepKey;
import com.beetloop.catalog.shared.error.ApiException;
import com.beetloop.catalog.shared.error.ErrorCode;
import com.beetloop.catalog.shared.model.EntryPath;
import com.beetloop.catalog.shared.model.Lifecycle;
import com.beetloop.catalog.shared.tenant.TenantContext;
import com.beetloop.catalog.shared.util.Ids;
import com.beetloop.catalog.shared.util.SequenceService;
import com.beetloop.catalog.template.TemplateService;
import com.beetloop.catalog.template.model.FormTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Draft creation, read and soft delete. Called when the vendor picks a category on "List a Product". */
@Service
public class ProductDraftService {

    private final ProductListingRepository repository;
    private final ProductGuard guard;
    private final TemplateService templates;
    private final SequenceService sequences;
    private final ProductRecalculator recalculator;

    public ProductDraftService(ProductListingRepository repository, ProductGuard guard,
                               TemplateService templates, SequenceService sequences,
                               ProductRecalculator recalculator) {
        this.repository = repository;
        this.guard = guard;
        this.templates = templates;
        this.sequences = sequences;
        this.recalculator = recalculator;
    }

    public ProductListing create(ProductDtos.CreateProductRequest request) {
        validateEntryPath(request);
        FormTemplate template = templates.active(request.categoryCode().name());

        Map<String, Object> data = new LinkedHashMap<>();
        for (String key : ProductStepKey.ALL) {
            data.put(key, new LinkedHashMap<String, Object>());
        }

        ProductListing listing = ProductListing.builder()
                .id(Ids.newId("prd"))
                .code(sequences.listingCode("PRD"))
                .vendorId(TenantContext.vendorId())
                .categoryCode(request.categoryCode())
                .categoryId(template.id())
                .entryPath(request.entryPath())
                .masterProductId(request.masterProductId())
                .requestCode(request.entryPath() == EntryPath.REQUEST_NEW
                        ? sequences.requestCode(request.categoryCode().abbreviation())
                        : null)
                .templateVersion(template.version())
                .currentStep(ProductStepKey.IDENTITY)
                .data(data)
                .lifecycle(Lifecycle.DRAFT)
                .build();

        recalculator.recompute(listing, template);
        return repository.save(listing);
    }

    private void validateEntryPath(ProductDtos.CreateProductRequest request) {
        if (request.entryPath() == EntryPath.MASTER
                && (request.masterProductId() == null || request.masterProductId().isBlank())) {
            throw new ApiException(ErrorCode.VALIDATION,
                    "masterProductId is required when entryPath is MASTER.");
        }
        if (request.entryPath() == EntryPath.REQUEST_NEW && request.masterProductId() != null) {
            throw new ApiException(ErrorCode.VALIDATION,
                    "masterProductId must be absent when entryPath is REQUEST_NEW.");
        }
    }

    public ProductListing get(String productId) {
        return guard.load(productId);
    }

    public void delete(String productId) {
        ProductListing listing = guard.loadEditable(productId);
        listing.setDeletedAt(Instant.now());
        repository.save(listing);
    }
}
