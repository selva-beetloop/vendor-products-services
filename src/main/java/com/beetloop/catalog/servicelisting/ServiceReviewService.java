package com.beetloop.catalog.servicelisting;

import com.beetloop.catalog.document.DocumentLinkService;
import com.beetloop.catalog.document.LibraryDocument;
import com.beetloop.catalog.servicelisting.dto.ServiceDtos;
import com.beetloop.catalog.servicelisting.model.ServiceConfiguration;
import com.beetloop.catalog.servicelisting.model.ServiceListing;
import com.beetloop.catalog.servicelisting.model.ServiceStepKey;
import com.beetloop.catalog.shared.error.FieldError;
import com.beetloop.catalog.shared.error.Warning;
import com.beetloop.catalog.shared.util.Maps;
import com.beetloop.catalog.template.TemplateService;
import com.beetloop.catalog.template.ValidationEngine;
import com.beetloop.catalog.template.ValidationMode;
import com.beetloop.catalog.template.ValidationResult;
import com.beetloop.catalog.template.model.FormTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ServiceReviewService {

    private final ServiceGuard guard;
    private final TemplateService templates;
    private final ValidationEngine validationEngine;
    private final ServiceRecalculator recalculator;
    private final DocumentLinkService linkService;

    public ServiceReviewService(ServiceGuard guard, TemplateService templates,
                                ValidationEngine validationEngine, ServiceRecalculator recalculator,
                                DocumentLinkService linkService) {
        this.guard = guard;
        this.templates = templates;
        this.validationEngine = validationEngine;
        this.recalculator = recalculator;
        this.linkService = linkService;
    }

    public ServiceDtos.ReviewResponse review(String serviceListingId) {
        ServiceListing listing = guard.load(serviceListingId);
        FormTemplate template = templates.forListing(listing.getCategoryCode().name(),
                listing.getTemplateVersion());

        List<ServiceDtos.StepPanel> panels = new ArrayList<>();
        List<FieldError> blocking = new ArrayList<>();
        int completed = 0;

        for (String stepKey : ServiceStepKey.COMPLETABLE) {
            FormTemplate.StepSchema step = template.step(stepKey).orElse(null);
            if (step == null) {
                continue;
            }
            List<FieldError> errors = errorsFor(listing, template, step, stepKey);
            boolean complete = errors.isEmpty();
            if (complete) {
                completed++;
            }
            blocking.addAll(errors);
            panels.add(new ServiceDtos.StepPanel(step.key(), step.label(),
                    List.of("Required", "Buyer-visible"), complete, errors.size(),
                    summarise(listing, stepKey), children(listing, stepKey), errors,
                    "/vendor/services/%s/steps/%s".formatted(listing.getId(), step.key())));
        }

        return new ServiceDtos.ReviewResponse(listing.getId(), listing.getCode(), template.label(),
                blocking.isEmpty(), completed, ServiceStepKey.COMPLETABLE.size(),
                "%d of %d steps complete".formatted(completed, ServiceStepKey.COMPLETABLE.size()),
                panels, blocking, List.of());
    }

    List<FieldError> errorsFor(ServiceListing listing, FormTemplate template,
                               FormTemplate.StepSchema step, String stepKey) {
        List<FieldError> errors = new ArrayList<>();
        switch (stepKey) {
            case ServiceStepKey.SELECT_SERVICE -> {
                if (recalculator.selectedServices(listing).isEmpty()) {
                    errors.add(FieldError.of(stepKey, "data.selectService.selectedServices",
                            "Selected services", "AT_LEAST_ONE_SERVICE_REQUIRED",
                            "Select at least one service before submitting."));
                }
            }
            case ServiceStepKey.CONFIGURE_SERVICES -> errors.addAll(configurationErrors(listing, template));
            case ServiceStepKey.COMPLIANCE -> errors.addAll(complianceErrors(listing));
            default -> {
                Map<String, Object> data = Maps.orEmpty(listing.step(stepKey));
                ValidationResult result = validationEngine.validateStep(template, step, data, data,
                        ValidationMode.SUBMIT);
                errors.addAll(result.errors());
            }
        }
        return errors;
    }

    private List<FieldError> configurationErrors(ServiceListing listing, FormTemplate template) {
        List<FieldError> errors = new ArrayList<>();
        if (listing.getConfigurations().isEmpty()) {
            errors.add(FieldError.of(ServiceStepKey.CONFIGURE_SERVICES, "configurations",
                    "Configurations", "AT_LEAST_ONE_SERVICE_REQUIRED",
                    "Configure at least one service before submitting."));
            return errors;
        }
        for (int i = 0; i < listing.getConfigurations().size(); i++) {
            ServiceConfiguration configuration = listing.getConfigurations().get(i);
            if ("CONFIGURED".equals(configuration.getConfigurationStatus())) {
                continue;
            }
            List<String> missing = new ArrayList<>();
            for (FormTemplate.SectionSchema section : template.childSections()) {
                String dataKey = ServiceRecalculator.dataKeyOf(section);
                if (!validationEngine.isSectionComplete(section,
                        Maps.orEmpty(configuration.section(dataKey)))) {
                    missing.add(dataKey);
                }
            }
            errors.add(new FieldError(ServiceStepKey.CONFIGURE_SERVICES,
                    "configurations[%d]".formatted(i), configuration.getName(),
                    "CONFIGURATION_NOT_COMPLETE",
                    "%s is %d%% configured.".formatted(
                            configuration.getName() == null ? "This service" : configuration.getName(),
                            configuration.getCompletionPercent()),
                    null, null,
                    Map.of("configurationId", configuration.getConfigurationId(),
                            "missingSections", missing)));
        }
        return errors;
    }

    /**
     * The only place expiry is ever checked. The UI renders DOCUMENT STATUS: Complete for
     * accreditations that expired in 2023.
     */
    private List<FieldError> complianceErrors(ServiceListing listing) {
        List<FieldError> errors = new ArrayList<>();
        List<LibraryDocument> expired = linkService.expiredDocuments(listing.getId());
        for (LibraryDocument document : expired) {
            errors.add(new FieldError(ServiceStepKey.COMPLIANCE, "data.compliance.links",
                    document.getName(), "DOCUMENT_EXPIRED",
                    "%s expired on %s. Replace or unlink it before submitting."
                            .formatted(document.getName(), document.getExpiryDate()),
                    null, null,
                    Map.of("libraryDocumentId", document.getId(),
                            "expiryDate", String.valueOf(document.getExpiryDate()))));
        }
        if (linkService.raw(listing.getId()).isEmpty()) {
            errors.add(FieldError.of(ServiceStepKey.COMPLIANCE, "data.compliance.links",
                    "Documents", "REQUIRED_DOCUMENT_MISSING",
                    "Link at least one accreditation or certification before submitting."));
        }
        return errors;
    }

    private List<ServiceDtos.SummaryItem> summarise(ServiceListing listing, String stepKey) {
        List<ServiceDtos.SummaryItem> items = new ArrayList<>();
        switch (stepKey) {
            case ServiceStepKey.SELECT_SERVICE -> items.add(new ServiceDtos.SummaryItem(
                    "Selected services", String.valueOf(recalculator.selectedServices(listing).size())));
            case ServiceStepKey.CONFIGURE_SERVICES -> {
                long configured = listing.getConfigurations().stream()
                        .filter(c -> "CONFIGURED".equals(c.getConfigurationStatus())).count();
                items.add(new ServiceDtos.SummaryItem("Configured",
                        "%d of %d".formatted(configured, listing.getConfigurations().size())));
            }
            case ServiceStepKey.COMPLIANCE -> {
                Map<String, Object> counters = Maps.asMap(
                        Maps.orEmpty(listing.step(ServiceStepKey.COMPLIANCE)).get("counters"));
                if (counters != null) {
                    counters.forEach((k, v) ->
                            items.add(new ServiceDtos.SummaryItem(k, String.valueOf(v))));
                }
            }
            default -> {
                // nothing to summarise
            }
        }
        return items;
    }

    private List<ServiceDtos.SummaryItem> children(ServiceListing listing, String stepKey) {
        if (!ServiceStepKey.CONFIGURE_SERVICES.equals(stepKey)) {
            return List.of();
        }
        return listing.getConfigurations().stream()
                .map(c -> new ServiceDtos.SummaryItem(c.getName(),
                        "%s - %d%%".formatted(c.getConfigurationStatus(), c.getCompletionPercent())))
                .toList();
    }

    public List<Warning> warnings(ServiceListing listing) {
        List<Warning> warnings = new ArrayList<>();
        linkService.expiredDocuments(listing.getId()).forEach(d -> warnings.add(
                Warning.of("data.compliance.links", "DOCUMENT_EXPIRED",
                        "%s expired on %s.".formatted(d.getName(), d.getExpiryDate()))));
        return warnings;
    }
}
