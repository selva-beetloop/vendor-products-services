package com.beetloop.catalog.servicelisting;

import com.beetloop.catalog.document.DocumentLinkService;
import com.beetloop.catalog.servicelisting.model.ServiceConfiguration;
import com.beetloop.catalog.servicelisting.model.ServiceListing;
import com.beetloop.catalog.servicelisting.model.ServiceStepKey;
import com.beetloop.catalog.shared.util.Keys;
import com.beetloop.catalog.shared.util.Maps;
import com.beetloop.catalog.template.DerivationService;
import com.beetloop.catalog.template.ValidationEngine;
import com.beetloop.catalog.template.model.FormTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * configurationStatus, configuredAt and completionPercent are DERIVED per configuration - the
 * Configuration Status column on the catalog grid is a rollup, never a stored field.
 */
@Component
public class ServiceRecalculator {

    private final ValidationEngine validationEngine;
    private final DerivationService derivation;
    private final DocumentLinkService linkService;

    public ServiceRecalculator(ValidationEngine validationEngine, DerivationService derivation,
                               DocumentLinkService linkService) {
        this.validationEngine = validationEngine;
        this.derivation = derivation;
        this.linkService = linkService;
    }

    public void recompute(ServiceListing listing, FormTemplate template) {
        recomputeConfigurations(listing, template);
        recomputeConfigureServicesStep(listing);
        recomputeComplianceStep(listing);
        recomputeCompletedSteps(listing, template);
        recomputeDerived(listing);
        recomputeSearchProjection(listing);
    }

    private void recomputeConfigurations(ServiceListing listing, FormTemplate template) {
        List<FormTemplate.SectionSchema> sections = template.childSections() == null
                ? List.of() : template.childSections();
        for (ServiceConfiguration configuration : listing.getConfigurations()) {
            if (configuration.getSections() == null) {
                configuration.setSections(new LinkedHashMap<>());
            }
            int complete = 0;
            for (FormTemplate.SectionSchema section : sections) {
                String dataKey = dataKeyOf(section);
                Map<String, Object> body = configuration.section(dataKey);
                if (body == null) {
                    continue;
                }
                deriveSection(dataKey, body, configuration);
                if (validationEngine.isSectionComplete(section, body)) {
                    complete++;
                }
            }
            int percent = derivation.percent(complete, Math.max(sections.size(), 1));
            configuration.setCompletionPercent(percent);
            String previous = configuration.getConfigurationStatus();
            configuration.setConfigurationStatus(percent >= 100 ? "CONFIGURED"
                    : percent > 0 ? "IN_PROCESS" : "NOT_CONFIGURED");
            if ("CONFIGURED".equals(configuration.getConfigurationStatus())
                    && !"CONFIGURED".equals(previous)) {
                configuration.setConfiguredAt(Instant.now());
            } else if (!"CONFIGURED".equals(configuration.getConfigurationStatus())) {
                configuration.setConfiguredAt(null);
            }
        }
    }

    public void deriveSection(String dataKey, Map<String, Object> body,
                              ServiceConfiguration configuration) {
        switch (dataKey) {
            case "technicalSpecification", "technicalSpecifications" -> {
                derivation.deriveSpecificationGroups(body, "analysisSpecifications");
                derivation.deriveAnalysisSummary(body,
                        configuration.section("configurePricingAndDetails"));
            }
            case "infrastructureOverview" -> {
                Map<String, Object> zone = Maps.mapAt(body, "zonePlotDefined");
                if (zone != null) {
                    derivation.deriveOccupancy(zone);
                }
            }
            default -> {
                // no derivation for this sub-step
            }
        }
    }

    private void recomputeConfigureServicesStep(ServiceListing listing) {
        Map<String, Object> step = listing.step(ServiceStepKey.CONFIGURE_SERVICES);
        if (step == null) {
            step = new LinkedHashMap<>();
            listing.putStep(ServiceStepKey.CONFIGURE_SERVICES, step);
        }
        long configured = listing.getConfigurations().stream()
                .filter(c -> "CONFIGURED".equals(c.getConfigurationStatus())).count();
        long inProcess = listing.getConfigurations().stream()
                .filter(c -> "IN_PROCESS".equals(c.getConfigurationStatus())).count();
        Map<String, Object> counters = new LinkedHashMap<>();
        counters.put("total", listing.getConfigurations().size());
        counters.put("configured", configured);
        counters.put("inProcess", inProcess);
        counters.put("notConfigured", listing.getConfigurations().size() - configured - inProcess);
        step.put("counters", counters);
        step.put("pagination", derivation.pagination(listing.getConfigurations().size()));
    }

    /** The per-service document table on step 3, plus the expiry counts the UI never computes. */
    private void recomputeComplianceStep(ServiceListing listing) {
        Map<String, Object> step = listing.step(ServiceStepKey.COMPLIANCE);
        if (step == null) {
            step = new LinkedHashMap<>();
            listing.putStep(ServiceStepKey.COMPLIANCE, step);
        }
        Map<String, Object> counters = linkService.rollup(listing.getId());
        step.put("counters", counters);

        List<Map<String, Object>> perService = new ArrayList<>();
        for (ServiceConfiguration configuration : listing.getConfigurations()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("selectionId", configuration.getSelectionId());
            row.put("name", configuration.getName());
            row.put("serviceType", configuration.getServiceType());
            long accreditations = 0;
            long certifications = 0;
            long supportDocs = 0;
            for (com.beetloop.catalog.document.DocumentLink link : linkService.raw(listing.getId())) {
                boolean applies = link.getSelectionId() == null
                        || link.getSelectionId().equals(configuration.getSelectionId());
                if (!applies) {
                    continue;
                }
                switch (link.getLinkType()) {
                    case "ACCREDITATION" -> accreditations++;
                    case "CERTIFICATION" -> certifications++;
                    default -> supportDocs++;
                }
            }
            row.put("accreditations", accreditations);
            row.put("certifications", certifications);
            row.put("supportDocs", supportDocs);
            row.put("documentStatus", accreditations + certifications + supportDocs > 0
                    ? "COMPLETE" : "INCOMPLETE");
            perService.add(row);
        }
        step.put("perService", perService);
    }

    private void recomputeCompletedSteps(ServiceListing listing, FormTemplate template) {
        List<String> completed = new ArrayList<>();
        for (String stepKey : ServiceStepKey.COMPLETABLE) {
            if (isStepComplete(listing, template, stepKey)) {
                completed.add(stepKey);
            }
        }
        listing.setCompletedSteps(completed);
    }

    public boolean isStepComplete(ServiceListing listing, FormTemplate template, String stepKey) {
        return switch (stepKey) {
            case ServiceStepKey.SELECT_SERVICE -> !selectedServices(listing).isEmpty();
            case ServiceStepKey.CONFIGURE_SERVICES -> !listing.getConfigurations().isEmpty()
                    && listing.getConfigurations().stream()
                            .allMatch(c -> "CONFIGURED".equals(c.getConfigurationStatus()));
            case ServiceStepKey.COMPLIANCE -> {
                Map<String, Object> counters =
                        Maps.asMap(Maps.orEmpty(listing.step(ServiceStepKey.COMPLIANCE)).get("counters"));
                if (counters == null) {
                    yield false;
                }
                long total = number(counters.get("accreditations")) + number(counters.get("certifications"))
                        + number(counters.get("supportDocs"));
                yield total > 0 && number(counters.get("expired")) == 0;
            }
            default -> template.step(stepKey)
                    .map(step -> validationEngine.isStepComplete(template, step, listing.step(stepKey)))
                    .orElse(false);
        };
    }

    private void recomputeDerived(ServiceListing listing) {
        Map<String, Object> derived = new LinkedHashMap<>();
        long configured = listing.getConfigurations().stream()
                .filter(c -> "CONFIGURED".equals(c.getConfigurationStatus())).count();
        derived.put("configurationCount", listing.getConfigurations().size());
        derived.put("configuredCount", configured);

        Map<String, Object> counters =
                Maps.asMap(Maps.orEmpty(listing.step(ServiceStepKey.COMPLIANCE)).get("counters"));
        if (counters != null) {
            derived.put("accreditationCount", counters.get("accreditations"));
            derived.put("certificationCount", counters.get("certifications"));
            derived.put("supportDocCount", counters.get("supportDocs"));
            derived.put("expiredDocumentCount", counters.get("expired"));
        }
        derived.put("completionPercent",
                derivation.percent(listing.getCompletedSteps().size(), ServiceStepKey.COMPLETABLE.size()));
        listing.setDerived(derived);
    }

    private void recomputeSearchProjection(ServiceListing listing) {
        Map<String, Object> search = new LinkedHashMap<>();
        List<ServiceConfiguration> configurations = listing.getConfigurations();
        String name = configurations.isEmpty() ? null : configurations.get(0).getName();
        if (name != null && configurations.size() > 1) {
            name = "%s (+%d more)".formatted(name, configurations.size() - 1);
        }
        search.put("name", name);
        search.put("serviceType", configurations.isEmpty() ? null : configurations.get(0).getServiceType());
        search.put("categoryRollup", listing.getCategoryCode() == null ? null
                : listing.getCategoryCode().name());
        search.put("configurationStatus", configurationStatusRollup(listing));
        search.put("keywords", keywords(listing));
        listing.setSearch(search);
    }

    /**
     * Configured / In Process / Not Configured, derived from how many selected services are complete -
     * a different axis from the Status column, which is the QC/publication state.
     */
    public String configurationStatusRollup(ServiceListing listing) {
        if (listing.getConfigurations().isEmpty()) {
            return "NOT_CONFIGURED";
        }
        long configured = listing.getConfigurations().stream()
                .filter(c -> "CONFIGURED".equals(c.getConfigurationStatus())).count();
        if (configured == listing.getConfigurations().size()) {
            return "CONFIGURED";
        }
        return configured > 0 || listing.getConfigurations().stream()
                .anyMatch(c -> "IN_PROCESS".equals(c.getConfigurationStatus()))
                ? "IN_PROCESS" : "NOT_CONFIGURED";
    }

    private List<String> keywords(ServiceListing listing) {
        Set<String> keywords = new LinkedHashSet<>();
        for (ServiceConfiguration configuration : listing.getConfigurations()) {
            if (configuration.getName() != null) {
                keywords.add(configuration.getName().toLowerCase());
            }
            Map<String, Object> marketplace = configuration.section("marketplaceAndSearch");
            Map<String, Object> indexing = marketplace == null ? null
                    : Maps.mapAt(marketplace, "searchAndIndexing");
            if (indexing != null) {
                List<Object> tags = Maps.asList(indexing.get("searchTagsAndKeywords"));
                if (tags != null) {
                    tags.forEach(t -> keywords.add(String.valueOf(t).toLowerCase()));
                }
            }
        }
        return new ArrayList<>(keywords);
    }

    public List<Map<String, Object>> selectedServices(ServiceListing listing) {
        Map<String, Object> step = listing.step(ServiceStepKey.SELECT_SERVICE);
        return step == null ? List.of() : Maps.asMapList(step.get("selectedServices"));
    }

    private long number(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    public static String dataKeyOf(FormTemplate.SectionSchema section) {
        return section.dataKey() != null ? section.dataKey() : Keys.toCamel(section.key());
    }
}
