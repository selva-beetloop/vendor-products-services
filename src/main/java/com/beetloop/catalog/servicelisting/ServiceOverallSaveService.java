package com.beetloop.catalog.servicelisting;

import com.beetloop.catalog.audit.AuditEvent;
import com.beetloop.catalog.audit.AuditService;
import com.beetloop.catalog.config.CatalogProperties;
import com.beetloop.catalog.servicelisting.dto.ServiceDtos;
import com.beetloop.catalog.servicelisting.model.ServiceConfiguration;
import com.beetloop.catalog.servicelisting.model.ServiceListing;
import com.beetloop.catalog.shared.error.ApiException;
import com.beetloop.catalog.shared.error.ErrorCode;
import com.beetloop.catalog.shared.error.FieldError;
import com.beetloop.catalog.shared.error.RejectedField;
import com.beetloop.catalog.shared.error.ValidationException;
import com.beetloop.catalog.shared.error.Warning;
import com.beetloop.catalog.shared.util.Ids;
import com.beetloop.catalog.shared.util.Maps;
import com.beetloop.catalog.template.TemplateService;
import com.beetloop.catalog.template.ValidationEngine;
import com.beetloop.catalog.template.ValidationMode;
import com.beetloop.catalog.template.ValidationResult;
import com.beetloop.catalog.template.model.FormTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * OVERALL SAVE for services. Same contract as products: full replace of all four steps AND the whole
 * configurations[] array, one transaction, qcStatus never touched.
 */
@Service
public class ServiceOverallSaveService {

    private final ServiceListingRepository repository;
    private final ServiceGuard guard;
    private final TemplateService templates;
    private final ValidationEngine validationEngine;
    private final ServiceRecalculator recalculator;
    private final ServiceDraftService draftService;
    private final CatalogProperties properties;
    private final AuditService audit;

    public ServiceOverallSaveService(ServiceListingRepository repository, ServiceGuard guard,
                                     TemplateService templates, ValidationEngine validationEngine,
                                     ServiceRecalculator recalculator, ServiceDraftService draftService,
                                     CatalogProperties properties, AuditService audit) {
        this.repository = repository;
        this.guard = guard;
        this.templates = templates;
        this.validationEngine = validationEngine;
        this.recalculator = recalculator;
        this.draftService = draftService;
        this.properties = properties;
        this.audit = audit;
    }

    @Transactional
    public ServiceDtos.SaveAllResponse saveAll(String pathId, ServiceDtos.SaveAllRequest request,
                                               String ifMatch, boolean ifMatchRequired) {
        String listingId = pathId != null ? pathId : request.serviceListingId();

        ServiceListing listing;
        boolean created = false;
        if (listingId == null) {
            listing = draftService.create(new ServiceDtos.CreateServiceRequest(request.categoryCode(), null));
            created = true;
        } else {
            listing = guard.loadEditable(listingId);
            guard.checkIfMatch(listing, ifMatch, ifMatchRequired);
            if (request.categoryCode() != null && request.categoryCode() != listing.getCategoryCode()) {
                throw new ApiException(ErrorCode.CATEGORY_IMMUTABLE,
                        "This listing is %s and cannot be changed to %s."
                                .formatted(listing.getCategoryCode(), request.categoryCode()));
            }
        }

        FormTemplate template = templates.forListing(listing.getCategoryCode().name(),
                listing.getTemplateVersion());

        List<FieldError> errors = new ArrayList<>();
        List<RejectedField> rejected = new ArrayList<>();
        List<Warning> warnings = new ArrayList<>();
        List<String> savedSteps = new ArrayList<>();
        List<String> clearedSteps = new ArrayList<>();
        Map<String, Map<String, Object>> incomingSteps = request.steps() == null ? Map.of() : request.steps();

        for (FormTemplate.StepSchema step : template.steps()) {
            String dataKey = ServiceStepSaveService.dataKey(step);
            Map<String, Object> incoming = incomingSteps.get(dataKey);
            if (incoming == null) {
                incoming = incomingSteps.get(step.key());
            }
            if (incoming == null) {
                if (!Maps.orEmpty(listing.step(dataKey)).isEmpty()) {
                    clearedSteps.add(dataKey);
                }
                listing.putStep(dataKey, new LinkedHashMap<>());
                continue;
            }
            ValidationResult result = validationEngine.validateStep(template, step, incoming,
                    Maps.orEmpty(listing.step(dataKey)), ValidationMode.SAVE, false);
            errors.addAll(result.errors());
            rejected.addAll(result.rejectedFields());
            warnings.addAll(result.warnings());
            listing.putStep(dataKey, result.sanitized());
            savedSteps.add(dataKey);
        }

        ChildOutcome outcome = replaceConfigurations(listing, template, request.configurations(),
                errors, rejected, warnings);

        if (!errors.isEmpty()) {
            throw new ValidationException(errors, warnings);
        }

        if (request.currentStep() != null && !request.currentStep().isBlank()) {
            listing.setCurrentStep(request.currentStep());
        }
        recalculator.recompute(listing, template);
        ServiceListing saved = repository.save(listing);

        if (!clearedSteps.isEmpty()) {
            warnings.add(Warning.of("steps", "STEPS_CLEARED",
                    ("%d step(s) were removed because they were absent from an overall save. "
                            + "Use PUT /steps/{stepKey} to update one step without affecting the others.")
                            .formatted(clearedSteps.size())));
        }
        if (!outcome.deleted().isEmpty()) {
            warnings.add(Warning.of("configurations", "CONFIGURATIONS_DELETED",
                    ("%d configuration(s) were removed because they were absent from an overall save. "
                            + "Use PUT /configurations/{id} to update one without affecting the others.")
                            .formatted(outcome.deleted().size())));
        }

        audit.record(AuditEvent.SERVICE_OVERALL_SAVED, "SERVICE_LISTING", saved.getId(),
                Map.of("savedSteps", savedSteps, "clearedSteps", clearedSteps,
                        "deletedConfigurations", outcome.deleted(), "created", created));

        return new ServiceDtos.SaveAllResponse(saved.getId(), saved.getCode(), saved.versionOrZero(),
                ServiceGuard.etag(saved), saved.getQcStatus(), saved.getLifecycle(), Instant.now(),
                savedSteps, clearedSteps, outcome.updated(), outcome.created(), outcome.deleted(),
                saved.getCompletedSteps(), saved.getDerived(), rejected, warnings);
    }

    private record ChildOutcome(List<ServiceDtos.SavedChild> updated,
                                List<ServiceDtos.SavedChild> created,
                                List<String> deleted) {
    }

    private ChildOutcome replaceConfigurations(ServiceListing listing, FormTemplate template,
                                               List<ServiceDtos.ConfigurationPayload> payloads,
                                               List<FieldError> errors, List<RejectedField> rejected,
                                               List<Warning> warnings) {
        List<ServiceDtos.ConfigurationPayload> incoming = payloads == null ? List.of() : payloads;
        int cap = properties.getLimits().getMaxConfigurationsPerListing();
        if (incoming.size() > cap) {
            throw new ApiException(ErrorCode.COLLECTION_CAP,
                    "A service listing may hold at most %d configurations; the request contained %d."
                            .formatted(cap, incoming.size()));
        }

        Map<String, ServiceConfiguration> existingById = new LinkedHashMap<>();
        listing.getConfigurations().forEach(c -> existingById.put(c.getConfigurationId(), c));

        List<ServiceConfiguration> rebuilt = new ArrayList<>();
        List<ServiceDtos.SavedChild> updated = new ArrayList<>();
        List<ServiceDtos.SavedChild> createdChildren = new ArrayList<>();
        Set<String> keptIds = new LinkedHashSet<>();
        Instant now = Instant.now();

        for (int index = 0; index < incoming.size(); index++) {
            ServiceDtos.ConfigurationPayload payload = incoming.get(index);
            ServiceConfiguration existing = payload.configurationId() == null ? null
                    : existingById.get(payload.configurationId());
            boolean isNew = existing == null;
            ServiceConfiguration configuration = isNew
                    ? ServiceConfiguration.builder()
                            .configurationId(Ids.newId("cfg"))
                            .selectionId(payload.selectionId() == null ? Ids.newId("sel")
                                    : payload.selectionId())
                            .sections(new LinkedHashMap<>())
                            .configurationStatus("NOT_CONFIGURED")
                            .createdAt(now).updatedAt(now).build()
                    : existing;

            Map<String, Object> sections = Maps.orEmpty(payload.sections());
            Map<String, Object> rebuiltSections = new LinkedHashMap<>();
            for (FormTemplate.SectionSchema section : template.childSections()) {
                String dataKey = ServiceRecalculator.dataKeyOf(section);
                if (!sections.containsKey(dataKey)) {
                    rebuiltSections.put(dataKey, new LinkedHashMap<String, Object>());
                    continue;
                }
                ValidationResult result = validationEngine.validateSection(section,
                        Maps.orEmpty(Maps.asMap(sections.get(dataKey))),
                        Maps.orEmpty(isNew ? Map.of() : configuration.section(dataKey)),
                        ValidationMode.SAVE, "configure-services",
                        "configurations[%s].sections.%s".formatted(configuration.getConfigurationId(),
                                dataKey));
                errors.addAll(result.errors());
                rejected.addAll(result.rejectedFields());
                warnings.addAll(result.warnings());
                rebuiltSections.put(dataKey, result.sanitized());
            }
            configuration.setSections(rebuiltSections);
            configuration.setUpdatedAt(now);
            hydrateNameFromSelection(listing, configuration, index);
            rebuilt.add(configuration);
            keptIds.add(configuration.getConfigurationId());
            if (isNew) {
                createdChildren.add(new ServiceDtos.SavedChild(configuration.getConfigurationId(), true,
                        null, null));
            } else {
                updated.add(new ServiceDtos.SavedChild(configuration.getConfigurationId(), false,
                        null, null));
            }
        }

        List<String> deleted = existingById.keySet().stream().filter(id -> !keptIds.contains(id)).toList();
        listing.setConfigurations(rebuilt);
        return new ChildOutcome(updated, createdChildren, deleted);
    }

    /**
     * Keeps the overview row's name in step with what step 1 selected. An overall save may be
     * authored offline, where the client cannot know the selectionId step 1 minted - so fall back
     * to pairing by position rather than leaving the configuration nameless.
     */
    private void hydrateNameFromSelection(ServiceListing listing, ServiceConfiguration configuration,
                                          int index) {
        List<Map<String, Object>> selected = recalculator.selectedServices(listing);
        Map<String, Object> match = selected.stream()
                .filter(row -> configuration.getSelectionId() != null
                        && configuration.getSelectionId().equals(row.get("selectionId")))
                .findFirst()
                .orElse(index < selected.size() ? selected.get(index) : null);
        if (match == null) {
            return;
        }
        configuration.setSelectionId(Maps.str(match, "selectionId"));
        configuration.setName(Maps.str(match, "name"));
        configuration.setServiceType(Maps.str(match, "serviceType"));
        configuration.setMasterServiceId(Maps.str(match, "masterServiceId"));
        configuration.setSource(Maps.str(match, "source"));
        configuration.setRequestCode(Maps.str(match, "requestCode"));
    }
}
