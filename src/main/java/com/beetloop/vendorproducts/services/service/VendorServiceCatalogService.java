package com.beetloop.vendorproducts.services.service;

import com.beetloop.vendorproducts.dto.PageResponse;
import com.beetloop.vendorproducts.exception.InvalidStateTransitionException;
import com.beetloop.vendorproducts.exception.ResourceNotFoundException;
import com.beetloop.vendorproducts.exception.ValidationException;
import com.beetloop.vendorproducts.services.domain.ServiceCategory;
import com.beetloop.vendorproducts.services.domain.ServiceDocument;
import com.beetloop.vendorproducts.services.domain.ServiceStatus;
import com.beetloop.vendorproducts.services.domain.VendorService;
import com.beetloop.vendorproducts.services.domain.VendorServiceBatch;
import com.beetloop.vendorproducts.services.dto.ServiceDtos;
import com.beetloop.vendorproducts.services.repository.VendorServiceBatchRepository;
import com.beetloop.vendorproducts.services.repository.VendorServiceRepository;
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
 * Orchestration for the Services wizard: draft creation, per-stage saves, the
 * transactional overall save, document CRUD, Submit for QC and QC decisions.
 */
@Service
public class VendorServiceCatalogService {

    private final VendorServiceBatchRepository batchRepository;
    private final VendorServiceRepository serviceRepository;
    private final ServiceValidationService validationService;
    private final ServiceMapper mapper;

    /**
     * Flush explicitly rather than calling {@code save()} on a managed entity:
     * {@code save()} performs a merge, and merging a graph containing a transient
     * child returns a managed <em>copy</em>, leaving the instance we still hold
     * without its generated id. Same trap the products module hit.
     */
    @PersistenceContext
    private EntityManager entityManager;

    public VendorServiceCatalogService(VendorServiceBatchRepository batchRepository,
                                       VendorServiceRepository serviceRepository,
                                       ServiceValidationService validationService,
                                       ServiceMapper mapper) {
        this.batchRepository = batchRepository;
        this.serviceRepository = serviceRepository;
        this.validationService = validationService;
        this.mapper = mapper;
    }

    // ------------------------------------------------------------------ create

    @Transactional
    public ServiceDtos.BatchResponse createBatch(ServiceDtos.CreateBatchRequest request,
                                                 String vendorId, String userId) {
        VendorServiceBatch batch = new VendorServiceBatch();
        batch.setCategory(request.category());
        batch.setVendorId(vendorId);
        batch.setCreatedBy(userId);
        batch.setStatus(ServiceStatus.DRAFT);
        return mapper.toBatchResponse(batchRepository.save(batch));
    }

    // ------------------------------------------------------------------- reads

    @Transactional(readOnly = true)
    public ServiceDtos.BatchResponse getBatch(UUID id) {
        return mapper.toBatchResponse(loadBatch(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ServiceDtos.ServiceSummaryResponse> listServices(String vendorId,
                                                                         ServiceCategory category,
                                                                         ServiceStatus status,
                                                                         String search,
                                                                         int page,
                                                                         int size,
                                                                         String sort) {
        Page<VendorService> result = serviceRepository.search(
                blankToNull(vendorId), category, status, blankToNull(search),
                PageRequest.of(Math.max(0, page), size < 1 ? 10 : size, parseSort(sort)));
        return PageResponse.of(result, mapper::toSummary);
    }

    @Transactional(readOnly = true)
    public PageResponse<ServiceDtos.ServiceSummaryResponse> qcQueue(String search, int page, int size, String sort) {
        Page<VendorService> result = serviceRepository.search(
                null, null, ServiceStatus.SUBMITTED_FOR_QC, blankToNull(search),
                PageRequest.of(Math.max(0, page), size < 1 ? 10 : size, parseSort(sort)));
        return PageResponse.of(result, mapper::toSummary);
    }

    // ------------------------------------------------------- step-based saves

    /**
     * Saves one wizard stage. Batch-level data goes on the batch; per-service
     * payloads go on each item, which is how Configure Services persists several
     * services from one screen.
     */
    @Transactional
    public ServiceDtos.BatchResponse saveStage(UUID batchId, String stageKey,
                                               ServiceDtos.StageSaveRequest request) {
        VendorServiceBatch batch = loadEditableBatch(batchId);
        boolean draft = request.isDraft();

        validationService.validateStage(batch.getCategory(), stageKey, request.dataOrEmpty(), draft);

        if (!request.dataOrEmpty().isEmpty()) {
            Map<String, Object> stages = batch.getStagePayloads() == null
                    ? new LinkedHashMap<>() : new LinkedHashMap<>(batch.getStagePayloads());
            stages.put(stageKey, new LinkedHashMap<>(request.dataOrEmpty()));
            batch.setStagePayloads(stages);
        }

        for (ServiceDtos.ItemStagePayload payload : request.itemsOrEmpty()) {
            validationService.validateStage(batch.getCategory(), stageKey, payload.dataOrEmpty(), draft);
            VendorService item = resolveItem(batch, payload);
            mapper.applyItemColumns(item, payload.sourceServiceId(), payload.custom(), payload.name(),
                    payload.sku(), payload.categoryLabel(), payload.serviceType(), payload.deliveryMode(),
                    payload.turnaround(), payload.region(), payload.thumbEmoji(), payload.configurationStatus());
            Map<String, Object> stages = item.getStagePayloads() == null
                    ? new LinkedHashMap<>() : new LinkedHashMap<>(item.getStagePayloads());
            stages.put(stageKey, new LinkedHashMap<>(payload.dataOrEmpty()));
            item.setStagePayloads(stages);
        }

        if (batch.getStatus() == ServiceStatus.DRAFT && !batch.getItems().isEmpty()) {
            batch.setStatus(ServiceStatus.CONFIGURED);
        }

        entityManager.flush();
        return mapper.toBatchResponse(batch);
    }

    /**
     * Finds the item a stage payload refers to, or creates one.
     *
     * <p>The client rarely knows our item id — the wizards identify a service by
     * the catalog id they started from — so {@code sourceServiceId} is the
     * natural key for an upsert. Without matching on it, every Save &amp; Continue
     * minted a fresh row and a three-step wizard left three copies of the same
     * service in the batch.
     *
     * <p>Only a non-blank id matches: custom services carry no source id, and
     * treating "no id" as a key would collapse every custom service into one.
     */
    private VendorService resolveItem(VendorServiceBatch batch, ServiceDtos.ItemStagePayload payload) {
        if (payload.id() != null && !payload.id().isBlank()) {
            return findItem(batch, UUID.fromString(payload.id()));
        }
        String sourceId = payload.sourceServiceId();
        if (sourceId != null && !sourceId.isBlank()) {
            for (VendorService existing : batch.getItems()) {
                if (sourceId.equals(existing.getSourceServiceId())) {
                    return existing;
                }
            }
        }
        return newItem(batch);
    }

    /** Removes one service from the batch. */
    @Transactional
    public void deleteItem(UUID batchId, UUID itemId) {
        VendorServiceBatch batch = loadEditableBatch(batchId);
        VendorService item = findItem(batch, itemId);
        batch.removeItem(item);
        int index = 0;
        for (VendorService remaining : batch.getItems()) {
            remaining.setPosition(index++);
        }
        entityManager.flush();
    }

    // ------------------------------------------------------------ overall save

    /** Whole-batch save in one transaction — either every section lands or none does. */
    @Transactional
    public ServiceDtos.BatchResponse saveAll(UUID batchId, ServiceDtos.OverallSaveRequest request) {
        VendorServiceBatch batch = loadEditableBatch(batchId);
        boolean draft = request.isDraft();

        for (Map.Entry<String, Map<String, Object>> entry : request.stagesOrEmpty().entrySet()) {
            validationService.validateStage(batch.getCategory(), entry.getKey(),
                    entry.getValue(), draft);
        }
        if (!request.stagesOrEmpty().isEmpty()) {
            Map<String, Object> stages = new LinkedHashMap<>();
            request.stagesOrEmpty().forEach(stages::put);
            batch.setStagePayloads(stages);
        }

        if (request.items() != null) {
            // Flush the orphan-removal before re-populating; clearing and refilling a
            // collection in one flush makes Hibernate re-insert rows it is deleting.
            batch.getItems().clear();
            entityManager.flush();
            for (ServiceDtos.ServiceItemRequest itemRequest : request.items()) {
                for (Map.Entry<String, Object> stage : itemRequest.stagePayloadsOrEmpty().entrySet()) {
                    if (stage.getValue() instanceof Map<?, ?> map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> typed = (Map<String, Object>) map;
                        validationService.validateStage(batch.getCategory(), stage.getKey(), typed, draft);
                    }
                }
                VendorService item = newItem(batch);
                mapper.applyItemColumns(item, itemRequest.sourceServiceId(), itemRequest.custom(),
                        itemRequest.name(), itemRequest.sku(), itemRequest.categoryLabel(),
                        itemRequest.serviceType(), itemRequest.deliveryMode(), itemRequest.turnaround(),
                        itemRequest.region(), itemRequest.thumbEmoji(), itemRequest.configurationStatus());
                item.setStagePayloads(new LinkedHashMap<>(itemRequest.stagePayloadsOrEmpty()));
                for (ServiceDtos.DocumentRequest documentRequest : itemRequest.documentsOrEmpty()) {
                    validationService.validateDocument(batch.getCategory(), documentRequest, draft);
                    item.addDocument(mapper.toDocument(documentRequest));
                }
            }
        }

        if (!draft) {
            validationService.validateCompleteBatch(batch.getCategory(),
                    batch.getStagePayloads(), batch.getItems().size());
        }

        if (request.isSubmitForQc()) {
            validationService.validateCompleteBatch(batch.getCategory(),
                    batch.getStagePayloads(), batch.getItems().size());
            transitionToSubmitted(batch);
        }

        entityManager.flush();
        return mapper.toBatchResponse(batch);
    }

    // ------------------------------------------------------------- documents

    @Transactional
    public ServiceDtos.ServiceResponse addDocument(UUID batchId, UUID itemId,
                                                   ServiceDtos.DocumentRequest request) {
        VendorServiceBatch batch = loadEditableBatch(batchId);
        VendorService item = findItem(batch, itemId);
        validationService.validateDocument(batch.getCategory(), request, false);
        item.addDocument(mapper.toDocument(request));
        entityManager.flush();
        return mapper.toServiceResponse(item);
    }

    @Transactional
    public ServiceDtos.ServiceResponse updateDocument(UUID batchId, UUID itemId, UUID documentId,
                                                      ServiceDtos.DocumentRequest request) {
        VendorServiceBatch batch = loadEditableBatch(batchId);
        VendorService item = findItem(batch, itemId);
        ServiceDocument document = item.getDocuments().stream()
                .filter(d -> documentId.equals(d.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Document " + documentId + " not found"));
        validationService.validateDocument(batch.getCategory(), request, false);
        mapper.applyDocument(document, request);
        entityManager.flush();
        return mapper.toServiceResponse(item);
    }

    @Transactional
    public ServiceDtos.ServiceResponse deleteDocument(UUID batchId, UUID itemId, UUID documentId) {
        VendorServiceBatch batch = loadEditableBatch(batchId);
        VendorService item = findItem(batch, itemId);
        ServiceDocument document = item.getDocuments().stream()
                .filter(d -> documentId.equals(d.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Document " + documentId + " not found"));
        item.removeDocument(document);
        int index = 0;
        for (ServiceDocument remaining : item.getDocuments()) {
            remaining.setPosition(index++);
        }
        entityManager.flush();
        return mapper.toServiceResponse(item);
    }

    // ------------------------------------------------------------ QC workflow

    /**
     * Submit for QC.
     *
     * <p>The wizard's final button is "Publish to Catalog" for ordinary entries and
     * "Submit for QC" for custom ones that came through the find flow, so a batch
     * containing any custom service routes to review; otherwise it publishes.
     */
    @Transactional
    public ServiceDtos.BatchResponse submit(UUID batchId) {
        VendorServiceBatch batch = loadBatch(batchId);
        if (batch.getStatus().isSubmitted()) {
            throw InvalidStateTransitionException.cannotSubmit(
                    com.beetloop.vendorproducts.domain.ProductStatus.SUBMITTED_FOR_QC);
        }
        validationService.validateCompleteBatch(batch.getCategory(),
                batch.getStagePayloads(), batch.getItems().size());
        transitionToSubmitted(batch);
        entityManager.flush();
        return mapper.toBatchResponse(batch);
    }

    @Transactional
    public ServiceDtos.BatchResponse qcDecision(UUID batchId, ServiceDtos.QcDecisionRequest request) {
        VendorServiceBatch batch = loadBatch(batchId);
        if (batch.getStatus() != ServiceStatus.SUBMITTED_FOR_QC
                && batch.getStatus() != ServiceStatus.PENDING_REVIEW) {
            throw new InvalidStateTransitionException(
                    "Service batch is not awaiting QC review (current status " + batch.getStatus().name() + ")");
        }

        String decision = request.decision().toUpperCase(Locale.ROOT);
        if (("REJECT".equals(decision) || "QUERY".equals(decision))
                && (request.remarks() == null || request.remarks().isBlank())) {
            throw new ValidationException("QC decision is incomplete",
                    List.of(new com.beetloop.vendorproducts.dto.ApiError.FieldError(
                            "remarks", "Remarks are required when rejecting or raising a query", null)));
        }

        switch (decision) {
            case "APPROVE" -> batch.setStatus(ServiceStatus.APPROVED);
            case "PUBLISH" -> batch.setStatus(ServiceStatus.PUBLISHED);
            case "REJECT" -> batch.setStatus(ServiceStatus.REJECTED);
            case "QUERY" -> batch.setStatus(ServiceStatus.QUERY);
            default -> throw new IllegalArgumentException("Unknown decision " + decision);
        }
        batch.setQcReviewer(request.reviewer());
        batch.setQcRemarks(request.remarks());
        batch.setReviewedAt(Instant.now());
        entityManager.flush();
        return mapper.toBatchResponse(batch);
    }

    @Transactional
    public void deleteBatch(UUID id) {
        batchRepository.delete(loadBatch(id));
    }

    // ------------------------------------------------------------- internals

    private VendorServiceBatch loadBatch(UUID id) {
        return batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service batch " + id + " not found"));
    }

    private VendorServiceBatch loadEditableBatch(UUID id) {
        VendorServiceBatch batch = loadBatch(id);
        ServiceStatus status = batch.getStatus();
        if (status == ServiceStatus.APPROVED || status == ServiceStatus.PUBLISHED
                || status == ServiceStatus.SUBMITTED_FOR_QC || status == ServiceStatus.PENDING_REVIEW) {
            throw new InvalidStateTransitionException(
                    "Service batch is " + status.getStatusLabel() + " and can no longer be edited.");
        }
        return batch;
    }

    private VendorService findItem(VendorServiceBatch batch, UUID itemId) {
        return batch.getItems().stream()
                .filter(i -> itemId.equals(i.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Service " + itemId + " not found"));
    }

    private VendorService newItem(VendorServiceBatch batch) {
        VendorService item = new VendorService();
        batch.addItem(item);
        return item;
    }

    private void transitionToSubmitted(VendorServiceBatch batch) {
        boolean anyCustom = batch.getItems().stream().anyMatch(VendorService::isCustom);
        batch.setStatus(anyCustom ? ServiceStatus.SUBMITTED_FOR_QC : ServiceStatus.PUBLISHED);
        batch.setSubmittedAt(Instant.now());
        batch.setReviewedAt(null);
        for (VendorService item : batch.getItems()) {
            if (item.getSku() == null || item.getSku().isBlank()) {
                item.setSku(generateSku(item.getName()));
            }
        }
    }

    /** Same vendor-SKU rule the products module uses: 3 letters of the first 2 words + "-001". */
    private String generateSku(String name) {
        String base = name == null ? "" : name;
        StringBuilder prefix = new StringBuilder();
        int used = 0;
        for (String word : base.trim().split("\\s+")) {
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
        return (prefix.isEmpty() ? "SVC" : prefix.toString()) + "-001";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "updatedAt");
        }
        String[] parts = sort.split(",");
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, parts[0].trim());
    }
}
