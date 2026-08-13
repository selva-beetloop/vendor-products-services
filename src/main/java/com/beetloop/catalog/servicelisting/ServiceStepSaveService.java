package com.beetloop.catalog.servicelisting;

import com.beetloop.catalog.audit.AuditEvent;
import com.beetloop.catalog.audit.AuditService;
import com.beetloop.catalog.servicelisting.dto.ServiceDtos;
import com.beetloop.catalog.servicelisting.model.ServiceListing;
import com.beetloop.catalog.servicelisting.model.ServiceStepKey;
import com.beetloop.catalog.shared.error.ValidationException;
import com.beetloop.catalog.shared.error.Warning;
import com.beetloop.catalog.shared.util.Ids;
import com.beetloop.catalog.shared.util.Keys;
import com.beetloop.catalog.shared.util.Maps;
import com.beetloop.catalog.shared.util.SequenceService;
import com.beetloop.catalog.template.TemplateService;
import com.beetloop.catalog.template.ValidationEngine;
import com.beetloop.catalog.template.ValidationMode;
import com.beetloop.catalog.template.ValidationResult;
import com.beetloop.catalog.template.model.FormTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * STEP-WISE SAVE for the four outer service steps: select-service, configure-services, compliance,
 * review. Same contract as products - one step, merged, qcStatus untouched.
 */
@Service
public class ServiceStepSaveService {

    private static final String PATH_B_BANNER =
            "Fill every field yourself - this is a new service request for Commercial Master. "
                    + "QC will review after you submit.";

    private final ServiceListingRepository repository;
    private final ServiceGuard guard;
    private final TemplateService templates;
    private final ValidationEngine validationEngine;
    private final ServiceRecalculator recalculator;
    private final SequenceService sequences;
    private final AuditService audit;

    public ServiceStepSaveService(ServiceListingRepository repository, ServiceGuard guard,
                                  TemplateService templates, ValidationEngine validationEngine,
                                  ServiceRecalculator recalculator, SequenceService sequences,
                                  AuditService audit) {
        this.repository = repository;
        this.guard = guard;
        this.templates = templates;
        this.validationEngine = validationEngine;
        this.recalculator = recalculator;
        this.sequences = sequences;
        this.audit = audit;
    }

    public ServiceDtos.StepReadResponse read(String serviceListingId, String stepKey) {
        ServiceListing listing = guard.load(serviceListingId);
        FormTemplate template = template(listing);
        FormTemplate.StepSchema step = templates.requireStep(template, stepKey);
        String dataKey = dataKey(step);
        return new ServiceDtos.StepReadResponse(listing.getId(), step.key(), listing.getTemplateVersion(),
                recalculator.isStepComplete(listing, template, dataKey),
                Maps.orEmpty(listing.step(dataKey)));
    }

    public ServiceDtos.StepSaveResponse save(String serviceListingId, String stepKey,
                                             ServiceDtos.StepSaveRequest request, String ifMatch) {
        ServiceListing listing = guard.loadEditable(serviceListingId);
        guard.checkIfMatch(listing, ifMatch, false);

        FormTemplate template = template(listing);
        FormTemplate.StepSchema step = templates.requireStep(template, stepKey);
        String dataKey = dataKey(step);

        ValidationResult result = validationEngine.validateStep(template, step,
                Maps.orEmpty(request.data()), Maps.orEmpty(listing.step(dataKey)), ValidationMode.SAVE);
        result.throwIfInvalid();

        Map<String, Object> sanitized = result.sanitized();
        String banner = null;
        if (ServiceStepKey.SELECT_SERVICE.equals(dataKey)) {
            banner = assignSelectionIds(listing, sanitized);
        }

        listing.putStep(dataKey, sanitized);
        if (request.currentStep() != null && !request.currentStep().isBlank()) {
            listing.setCurrentStep(request.currentStep());
        }
        recalculator.recompute(listing, template);
        ServiceListing saved = repository.save(listing);

        audit.record(AuditEvent.SERVICE_STEP_SAVED, "SERVICE_LISTING", saved.getId(),
                Map.of("stepKey", step.key(), "toVersion", saved.versionOrZero()));

        List<Warning> warnings = new ArrayList<>(result.warnings());
        saved.getConfigurations().stream()
                .filter(c -> !"CONFIGURED".equals(c.getConfigurationStatus()))
                .forEach(c -> warnings.add(Warning.of("configurations[" + c.getConfigurationId() + "]",
                        "CONFIGURATION_INCOMPLETE",
                        "%s is %d%% complete and will block submission."
                                .formatted(c.getName(), c.getCompletionPercent()))));

        return new ServiceDtos.StepSaveResponse(saved.getId(), step.key(), saved.versionOrZero(),
                ServiceGuard.etag(saved), saved.getCurrentStep(), saved.getCompletedSteps(),
                saved.getQcStatus(), Instant.now(), Maps.orEmpty(saved.step(dataKey)), banner,
                result.rejectedFields(), warnings);
    }

    public ServiceDtos.ValidateStepResponse validate(String serviceListingId, String stepKey,
                                                     ServiceDtos.StepSaveRequest request) {
        ServiceListing listing = guard.load(serviceListingId);
        FormTemplate template = template(listing);
        FormTemplate.StepSchema step = templates.requireStep(template, stepKey);
        ValidationResult result = validationEngine.validateStep(template, step,
                Maps.orEmpty(request.data()), Maps.orEmpty(listing.step(dataKey(step))),
                ValidationMode.SUBMIT);
        return new ServiceDtos.ValidateStepResponse(step.key(), result.valid(), result.valid(),
                result.valid() ? null : ValidationException.DEFAULT_BANNER,
                result.errors(), result.warnings());
    }

    /**
     * Mints a selectionId per selected service, and a Request Code for Path B. Path A rows carry
     * readOnlyFields - the sub-step subtitle literally reads "Commercial Master (view only)".
     */
    private String assignSelectionIds(ServiceListing listing, Map<String, Object> step) {
        List<Map<String, Object>> selected = Maps.asMapList(step.get("selectedServices"));
        boolean requestNew = "REQUEST_NEW".equals(step.get("entryPath"));
        for (Map<String, Object> row : selected) {
            row.putIfAbsent("selectionId", Ids.newId("sel"));
            if (requestNew || row.get("masterServiceId") == null) {
                row.putIfAbsent("requestCode",
                        sequences.requestCode(listing.getCategoryCode().abbreviation()));
                row.put("readOnlyFields", List.of());
            } else {
                row.put("requestCode", null);
                row.put("readOnlyFields", List.of("serviceName", "primaryTestMethod", "resultUnit"));
            }
        }
        return requestNew ? PATH_B_BANNER : null;
    }

    FormTemplate template(ServiceListing listing) {
        return templates.forListing(listing.getCategoryCode().name(), listing.getTemplateVersion());
    }

    static String dataKey(FormTemplate.StepSchema step) {
        return step.dataKey() != null ? step.dataKey() : Keys.toCamel(step.key());
    }
}
