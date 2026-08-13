package com.beetloop.catalog.servicelisting;

import com.beetloop.catalog.servicelisting.dto.ServiceDtos;
import com.beetloop.catalog.servicelisting.model.ServiceListing;
import com.beetloop.catalog.servicelisting.model.ServiceStepKey;
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

@Service
public class ServiceDraftService {

    private final ServiceListingRepository repository;
    private final ServiceGuard guard;
    private final TemplateService templates;
    private final SequenceService sequences;
    private final ServiceRecalculator recalculator;

    public ServiceDraftService(ServiceListingRepository repository, ServiceGuard guard,
                               TemplateService templates, SequenceService sequences,
                               ServiceRecalculator recalculator) {
        this.repository = repository;
        this.guard = guard;
        this.templates = templates;
        this.sequences = sequences;
        this.recalculator = recalculator;
    }

    public ServiceListing create(ServiceDtos.CreateServiceRequest request) {
        FormTemplate template = templates.active(request.categoryCode().name());
        Map<String, Object> data = new LinkedHashMap<>();
        for (String key : ServiceStepKey.ALL) {
            data.put(key, new LinkedHashMap<String, Object>());
        }
        ServiceListing listing = ServiceListing.builder()
                .id(Ids.newId("svc"))
                .code(sequences.listingCode("SVC"))
                .vendorId(TenantContext.vendorId())
                .categoryCode(request.categoryCode())
                .categoryId(template.id())
                .entryPath(request.entryPath() == null ? EntryPath.MASTER : request.entryPath())
                .templateVersion(template.version())
                .currentStep("select-service")
                .data(data)
                .lifecycle(Lifecycle.DRAFT)
                .build();
        recalculator.recompute(listing, template);
        return repository.save(listing);
    }

    public ServiceListing get(String serviceListingId) {
        return guard.load(serviceListingId);
    }

    public FormTemplate template(ServiceListing listing) {
        return templates.forListing(listing.getCategoryCode().name(), listing.getTemplateVersion());
    }

    public void delete(String serviceListingId) {
        ServiceListing listing = guard.loadEditable(serviceListingId);
        listing.setDeletedAt(Instant.now());
        repository.save(listing);
    }
}
