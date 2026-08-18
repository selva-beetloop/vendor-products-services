package com.beetloop.vendorproducts.services.service;

import com.beetloop.vendorproducts.services.domain.ServiceDocument;
import com.beetloop.vendorproducts.services.domain.VendorService;
import com.beetloop.vendorproducts.services.domain.VendorServiceBatch;
import com.beetloop.vendorproducts.services.dto.ServiceDtos;
import com.beetloop.vendorproducts.services.registry.ServiceFieldRegistry;
import com.beetloop.vendorproducts.storage.DataUriPersister;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Entity ↔ DTO translation for the Services module. */
@Component
public class ServiceMapper {

    private final ServiceFieldRegistry registry;
    private final DataUriPersister dataUriPersister;

    public ServiceMapper(ServiceFieldRegistry registry, DataUriPersister dataUriPersister) {
        this.registry = registry;
        this.dataUriPersister = dataUriPersister;
    }

    public ServiceDtos.BatchResponse toBatchResponse(VendorServiceBatch batch) {
        return new ServiceDtos.BatchResponse(
                batch.getId().toString(),
                batch.getCategory().getId(),
                batch.getCategory().getLabel(),
                batch.getCategory().getGroupId(),
                batch.getStatus().name(),
                batch.getStatus().getStatusKind(),
                batch.getStatus().getStatusLabel(),
                batch.getCategory().getStageCount(),
                registry.stageKeys(batch.getCategory()),
                batch.getStagePayloads() == null ? Map.of() : batch.getStagePayloads(),
                batch.getItems().stream().map(this::toServiceResponse).toList(),
                new ServiceDtos.BatchResponse.QcSection(batch.getQcReviewer(), batch.getQcRemarks(),
                        batch.getSubmittedAt(), batch.getReviewedAt()),
                batch.getCreatedAt(),
                batch.getUpdatedAt());
    }

    public ServiceDtos.ServiceResponse toServiceResponse(VendorService service) {
        return new ServiceDtos.ServiceResponse(
                service.getId().toString(),
                service.getPosition(),
                service.getSourceServiceId(),
                service.isCustom(),
                service.getName(),
                service.getSku(),
                service.getCategoryLabel(),
                service.getServiceType(),
                service.getDeliveryMode(),
                service.getTurnaround(),
                service.getRegion(),
                service.getThumbEmoji(),
                service.getConfigurationStatus(),
                service.getRfqs(),
                service.getStagePayloads() == null ? Map.of() : service.getStagePayloads(),
                service.getDocuments().stream().map(this::toDocumentResponse).toList(),
                service.getCreatedAt(),
                service.getUpdatedAt());
    }

    public ServiceDtos.DocumentResponse toDocumentResponse(ServiceDocument document) {
        return new ServiceDtos.DocumentResponse(
                document.getId().toString(),
                document.getExternalRef(),
                document.getPosition(),
                document.getKind().name(),
                document.getName(),
                document.getIssuingBody(),
                document.getReferenceNumber(),
                document.getValidFrom(),
                document.getValidTo(),
                document.getStatus(),
                document.getFileName(),
                document.getFileId(),
                document.getFileUrl(),
                document.getData() == null ? Map.of() : document.getData());
    }

    /** One services-listing row, shaped like the frontend's CatalogService. */
    public ServiceDtos.ServiceSummaryResponse toSummary(VendorService service) {
        VendorServiceBatch batch = service.getBatch();
        return new ServiceDtos.ServiceSummaryResponse(
                service.getId().toString(),
                batch.getId().toString(),
                service.getSourceServiceId(),
                service.getName(),
                service.getSku(),
                service.getCategoryLabel() != null ? service.getCategoryLabel() : batch.getCategory().getLabel(),
                service.getServiceType(),
                service.getDeliveryMode(),
                service.getTurnaround(),
                service.getRegion(),
                batch.getStatus().getStatusKind(),
                batch.getStatus().getStatusLabel(),
                batch.getStatus().name(),
                service.getRfqs(),
                batch.getStatus().isSubmitted() ? "eye" : "edit",
                service.getThumbEmoji(),
                service.getConfigurationStatus(),
                batch.getCategory().getId(),
                batch.getCategory().getGroupId(),
                service.getDocuments().size(),
                service.getCreatedAt(),
                service.getUpdatedAt());
    }

    // ---- DTO → entity ----

    /** Applies the listing/summary columns present on a request; nulls are left alone. */
    public void applyItemColumns(VendorService target,
                                 String sourceServiceId,
                                 Boolean custom,
                                 String name,
                                 String sku,
                                 String categoryLabel,
                                 String serviceType,
                                 String deliveryMode,
                                 String turnaround,
                                 String region,
                                 String thumbEmoji,
                                 String configurationStatus) {
        if (sourceServiceId != null) {
            target.setSourceServiceId(sourceServiceId);
            // Vendor-created entries are prefixed custom-… by the wizard; that flag
            // decides whether submit routes to QC or straight to publish.
            if (custom == null) {
                target.setCustom(CustomSourceIds.isCustom(sourceServiceId));
            }
        }
        if (custom != null) {
            target.setCustom(custom);
        }
        if (name != null) {
            target.setName(name);
        }
        if (sku != null) {
            target.setSku(sku);
        }
        if (categoryLabel != null) {
            target.setCategoryLabel(categoryLabel);
        }
        if (serviceType != null) {
            target.setServiceType(serviceType);
        }
        if (deliveryMode != null) {
            target.setDeliveryMode(deliveryMode);
        }
        if (turnaround != null) {
            target.setTurnaround(turnaround);
        }
        if (region != null) {
            target.setRegion(region);
        }
        if (thumbEmoji != null) {
            target.setThumbEmoji(thumbEmoji);
        }
        if (configurationStatus != null) {
            target.setConfigurationStatus(configurationStatus);
        }
    }

    public ServiceDocument toDocument(ServiceDtos.DocumentRequest request) {
        ServiceDocument document = new ServiceDocument();
        applyDocument(document, request);
        return document;
    }

    public void applyDocument(ServiceDocument target, ServiceDtos.DocumentRequest request) {
        target.setKind(ServiceDocument.Kind.from(request.kind()));
        // `id` is the client's own row id, not ours — keep it as a correlation ref.
        if (request.id() != null && !request.id().isBlank()) {
            target.setExternalRef(request.id());
        }
        target.setName(request.name());
        target.setIssuingBody(request.issuingBody());
        target.setReferenceNumber(request.referenceNumber());
        target.setValidFrom(request.validFrom());
        target.setValidTo(request.validTo());
        target.setStatus(request.status());
        target.setFileName(request.fileName());
        target.setFileId(request.fileId());
        target.setFileUrl(request.fileUrl());
        persistEmbeddedFile(target);
        target.setData(new LinkedHashMap<>(request.dataOrEmpty()));
    }

    private void persistEmbeddedFile(ServiceDocument target) {
        DataUriPersister.StoredRef stored = dataUriPersister.persist(
                target.getFileUrl(), target.getFileName(), "service-document");
        if (stored == null) {
            return;
        }
        target.setFileId(stored.id());
        target.setFileUrl(stored.url());
        if (stored.fileName() != null) {
            target.setFileName(stored.fileName());
        }
    }

    public List<ServiceDocument> toDocuments(List<ServiceDtos.DocumentRequest> requests) {
        List<ServiceDocument> out = new ArrayList<>();
        for (ServiceDtos.DocumentRequest request : requests) {
            out.add(toDocument(request));
        }
        return out;
    }
}
