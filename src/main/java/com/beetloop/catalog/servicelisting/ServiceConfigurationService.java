package com.beetloop.catalog.servicelisting;

import com.beetloop.catalog.audit.AuditEvent;
import com.beetloop.catalog.audit.AuditService;
import com.beetloop.catalog.config.CatalogProperties;
import com.beetloop.catalog.servicelisting.dto.ServiceDtos;
import com.beetloop.catalog.servicelisting.model.ServiceConfiguration;
import com.beetloop.catalog.servicelisting.model.ServiceListing;
import com.beetloop.catalog.servicelisting.model.ServiceStepKey;
import com.beetloop.catalog.shared.api.PageMeta;
import com.beetloop.catalog.shared.api.PagedResponse;
import com.beetloop.catalog.shared.error.ApiException;
import com.beetloop.catalog.shared.error.ErrorCode;
import com.beetloop.catalog.shared.error.FieldError;
import com.beetloop.catalog.shared.error.RejectedField;
import com.beetloop.catalog.shared.error.ValidationException;
import com.beetloop.catalog.shared.error.Warning;
import com.beetloop.catalog.shared.util.Ids;
import com.beetloop.catalog.shared.util.Maps;
import com.beetloop.catalog.shared.util.SequenceService;
import com.beetloop.catalog.template.TemplateService;
import com.beetloop.catalog.template.ValidationEngine;
import com.beetloop.catalog.template.ValidationMode;
import com.beetloop.catalog.template.ValidationResult;
import com.beetloop.catalog.template.model.FormTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Per-service configuration CRUD plus the sub-step SECTION SAVE. */
@Service
public class ServiceConfigurationService {

    private static final DateTimeFormatter STATUS_LINE =
            DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH).withZone(ZoneId.systemDefault());

    private final ServiceListingRepository repository;
    private final ServiceGuard guard;
    private final TemplateService templates;
    private final ValidationEngine validationEngine;
    private final ServiceRecalculator recalculator;
    private final SequenceService sequences;
    private final CatalogProperties properties;
    private final AuditService audit;

    public ServiceConfigurationService(ServiceListingRepository repository, ServiceGuard guard,
                                       TemplateService templates, ValidationEngine validationEngine,
                                       ServiceRecalculator recalculator, SequenceService sequences,
                                       CatalogProperties properties, AuditService audit) {
        this.repository = repository;
        this.guard = guard;
        this.templates = templates;
        this.validationEngine = validationEngine;
        this.recalculator = recalculator;
        this.sequences = sequences;
        this.properties = properties;
        this.audit = audit;
    }

    /** Drives the "Selected Services (N)" table on outer step 2. */
    public PagedResponse<ServiceDtos.ConfigurationRow> list(String serviceListingId, int page, int size) {
        ServiceListing listing = guard.load(serviceListingId);
        FormTemplate template = template(listing);
        List<ServiceConfiguration> all = listing.getConfigurations();

        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        List<ServiceDtos.ConfigurationRow> rows = new ArrayList<>();
        for (int i = from; i < to; i++) {
            rows.add(toRow(listing, all.get(i), i + 1));
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("counters", Maps.asMap(
                Maps.orEmpty(listing.step(ServiceStepKey.CONFIGURE_SERVICES)).get("counters")));
        meta.put("sectionKeys", template.childSectionKeys());
        return PagedResponse.of(rows, PageMeta.of(page, size, all.size()), meta);
    }

    public ServiceDtos.ConfigurationResponse get(String serviceListingId, String configurationId) {
        ServiceListing listing = guard.load(serviceListingId);
        return ServiceMapper.toConfigurationResponse(require(listing, configurationId), template(listing));
    }

    /** "Add new service" - either linking a Commercial Master row (Path A) or requesting a new one (Path B). */
    public ServiceDtos.ConfigurationResponse add(String serviceListingId,
                                                 ServiceDtos.AddConfigurationRequest request) {
        ServiceListing listing = guard.loadEditable(serviceListingId);
        FormTemplate template = template(listing);
        int cap = properties.getLimits().getMaxConfigurationsPerListing();
        if (listing.getConfigurations().size() >= cap) {
            throw new ApiException(ErrorCode.COLLECTION_CAP,
                    "A service listing may hold at most %d configurations.".formatted(cap));
        }

        boolean pathB = Boolean.TRUE.equals(request.requestNew()) || request.masterServiceId() == null;
        Instant now = Instant.now();
        ServiceConfiguration configuration = ServiceConfiguration.builder()
                .configurationId(Ids.newId("cfg"))
                .selectionId(Ids.newId("sel"))
                .masterServiceId(request.masterServiceId())
                .requestCode(pathB ? sequences.requestCode(listing.getCategoryCode().abbreviation()) : null)
                .name(request.name())
                .serviceType(request.serviceType())
                .source(pathB ? "VENDOR_REQUEST" : "COMMERCIAL_MASTER")
                .sections(emptySections(template))
                .configurationStatus("NOT_CONFIGURED")
                .createdAt(now)
                .updatedAt(now)
                .build();

        listing.getConfigurations().add(configuration);
        mirrorIntoSelectServiceStep(listing, configuration);
        recalculator.recompute(listing, template);
        repository.save(listing);
        return ServiceMapper.toConfigurationResponse(configuration, template);
    }

    /**
     * SECTION SAVE. Fires on "Save & Continue" inside a configuration; on the final sub-step the UI's
     * "Save & Return to Overview" is the same call, the navigation is client-side.
     */
    public ServiceDtos.SectionSaveResponse saveSection(String serviceListingId, String configurationId,
                                                       String sectionKey,
                                                       ServiceDtos.SectionSaveRequest request) {
        ServiceListing listing = guard.loadEditable(serviceListingId);
        FormTemplate template = template(listing);
        FormTemplate.SectionSchema section = templates.requireChildSection(template, sectionKey);
        ServiceConfiguration configuration = require(listing, configurationId);

        String dataKey = ServiceRecalculator.dataKeyOf(section);
        ValidationResult result = validationEngine.validateSection(section, Maps.orEmpty(request.data()),
                Maps.orEmpty(configuration.section(dataKey)), ValidationMode.SAVE,
                "configure-services",
                "configurations[%s].sections.%s".formatted(configurationId, dataKey));
        result.throwIfInvalid();

        configuration.getSections().put(dataKey, result.sanitized());
        configuration.setUpdatedAt(Instant.now());
        recalculator.recompute(listing, template);
        ServiceListing saved = repository.save(listing);
        ServiceConfiguration reloaded = require(saved, configurationId);

        audit.record(AuditEvent.CONFIGURATION_SECTION_SAVED, "SERVICE_LISTING", saved.getId(),
                Map.of("configurationId", configurationId, "sectionKey", section.key()));

        return new ServiceDtos.SectionSaveResponse(configurationId, section.key(), saved.versionOrZero(),
                reloaded.getCompletionPercent(), reloaded.getConfigurationStatus(),
                Maps.orEmpty(reloaded.section(dataKey)),
                dependentSchema(template, dataKey, result.sanitized()),
                result.rejectedFields(), result.warnings());
    }

    public ServiceDtos.ConfigurationWholeSaveResponse saveWhole(
            String serviceListingId, String configurationId,
            ServiceDtos.ConfigurationWholeSaveRequest request) {
        ServiceListing listing = guard.loadEditable(serviceListingId);
        FormTemplate template = template(listing);
        ServiceConfiguration configuration = require(listing, configurationId);

        List<FieldError> errors = new ArrayList<>();
        List<RejectedField> rejected = new ArrayList<>();
        List<Warning> warnings = new ArrayList<>();
        List<String> savedSections = new ArrayList<>();
        Map<String, Object> incoming = Maps.orEmpty(request.sections());
        Map<String, Object> rebuilt = new LinkedHashMap<>();

        for (FormTemplate.SectionSchema section : template.childSections()) {
            String dataKey = ServiceRecalculator.dataKeyOf(section);
            if (!incoming.containsKey(dataKey)) {
                continue;
            }
            ValidationResult result = validationEngine.validateSection(section,
                    Maps.orEmpty(Maps.asMap(incoming.get(dataKey))),
                    Maps.orEmpty(configuration.section(dataKey)), ValidationMode.SAVE,
                    "configure-services",
                    "configurations[%s].sections.%s".formatted(configurationId, dataKey));
            errors.addAll(result.errors());
            rejected.addAll(result.rejectedFields());
            warnings.addAll(result.warnings());
            rebuilt.put(dataKey, result.sanitized());
            savedSections.add(dataKey);
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors, warnings);
        }

        rebuilt.forEach(configuration.getSections()::put);
        configuration.setUpdatedAt(Instant.now());
        recalculator.recompute(listing, template);
        ServiceListing saved = repository.save(listing);
        ServiceConfiguration reloaded = require(saved, configurationId);

        return new ServiceDtos.ConfigurationWholeSaveResponse(configurationId, saved.versionOrZero(),
                reloaded.getCompletionPercent(), reloaded.getConfigurationStatus(), savedSections,
                rejected, warnings);
    }

    public void delete(String serviceListingId, String configurationId) {
        ServiceListing listing = guard.loadEditable(serviceListingId);
        FormTemplate template = template(listing);
        ServiceConfiguration configuration = require(listing, configurationId);
        listing.getConfigurations().remove(configuration);
        removeFromSelectServiceStep(listing, configuration.getSelectionId());
        recalculator.recompute(listing, template);
        repository.save(listing);
    }

    // ------------------------------------------------------------------ internals

    /**
     * Agro Cluster documents this itself: "Determines the structure of Step 2 - clusters are
     * commodity-defined, food parks are zone/plot-defined." So the response tells the client which
     * sub-step 2 schema now applies.
     */
    private Map<String, Object> dependentSchema(FormTemplate template, String dataKey,
                                                Map<String, Object> saved) {
        if (!"serviceDetails".equals(dataKey)) {
            return null;
        }
        Map<String, Object> identity = Maps.mapAt(saved, "providerSchemeIdentity");
        Object providerType = identity == null ? null : identity.get("infrastructureProviderType");
        if (providerType == null) {
            return null;
        }
        String variant = switch (String.valueOf(providerType)) {
            case "MEGA_FOOD_PARK_MFPS", "FOOD_PROCESSING_INDUSTRIAL_ESTATE" -> "zonePlotDefined";
            default -> "commodityDefined";
        };
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sectionKey", "infrastructure-overview");
        payload.put("resolvedVariant", variant);
        payload.put("reason", "infrastructureProviderType = " + providerType);
        payload.put("templateUrl", "/api/v1/masters/form-templates/%s?version=%d#infrastructure-overview.%s"
                .formatted(template.categoryCode(), template.version(), variant));
        return payload;
    }

    private Map<String, Object> emptySections(FormTemplate template) {
        Map<String, Object> sections = new LinkedHashMap<>();
        for (FormTemplate.SectionSchema section : template.childSections()) {
            sections.put(ServiceRecalculator.dataKeyOf(section), new LinkedHashMap<String, Object>());
        }
        return sections;
    }

    private void mirrorIntoSelectServiceStep(ServiceListing listing, ServiceConfiguration configuration) {
        Map<String, Object> step = listing.step(ServiceStepKey.SELECT_SERVICE);
        if (step == null) {
            step = new LinkedHashMap<>();
            listing.putStep(ServiceStepKey.SELECT_SERVICE, step);
        }
        List<Map<String, Object>> selected = new ArrayList<>(Maps.asMapList(step.get("selectedServices")));
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("selectionId", configuration.getSelectionId());
        row.put("masterServiceId", configuration.getMasterServiceId());
        row.put("requestCode", configuration.getRequestCode());
        row.put("name", configuration.getName());
        row.put("serviceType", configuration.getServiceType());
        row.put("source", configuration.getSource());
        selected.add(row);
        step.put("selectedServices", selected);
        step.putIfAbsent("entryPath", configuration.getMasterServiceId() == null
                ? "REQUEST_NEW" : "MASTER");
    }

    private void removeFromSelectServiceStep(ServiceListing listing, String selectionId) {
        Map<String, Object> step = listing.step(ServiceStepKey.SELECT_SERVICE);
        if (step == null) {
            return;
        }
        List<Map<String, Object>> selected = new ArrayList<>(Maps.asMapList(step.get("selectedServices")));
        selected.removeIf(row -> Objects.equals(row.get("selectionId"), selectionId));
        step.put("selectedServices", selected);
    }

    private ServiceDtos.ConfigurationRow toRow(ServiceListing listing, ServiceConfiguration c, int index) {
        List<String> analytes = List.of();
        Map<String, Object> details = c.section("serviceDetails");
        Map<String, Object> definition = details == null ? null : Maps.mapAt(details, "serviceDefinition");
        String tat = null;
        if (definition != null) {
            List<Object> raw = Maps.asList(definition.get("keyParametersAnalytes"));
            if (raw != null) {
                analytes = raw.stream().map(String::valueOf).toList();
            }
            tat = Maps.str(definition, "typicalTurnaroundTime");
        }
        String statusLine = "CONFIGURED".equals(c.getConfigurationStatus()) && c.getConfiguredAt() != null
                ? "Completed - Configured on " + STATUS_LINE.format(c.getConfiguredAt())
                : "%d%% complete".formatted(c.getCompletionPercent());
        return new ServiceDtos.ConfigurationRow(
                c.getConfigurationId(), c.getSelectionId(), index, c.getName(), c.getServiceType(),
                c.getSource(), "COMMERCIAL_MASTER".equals(c.getSource()) ? "Commercial Master"
                        : "Vendor Request",
                analytes, tat, c.getConfigurationStatus(), c.getConfiguredAt(), statusLine,
                c.getCompletionPercent(),
                "/vendor/services/%s/configurations/%s".formatted(listing.getId(), c.getConfigurationId()));
    }

    ServiceConfiguration require(ServiceListing listing, String configurationId) {
        return listing.getConfigurations().stream()
                .filter(c -> Objects.equals(c.getConfigurationId(), configurationId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Configuration " + configurationId));
    }

    private FormTemplate template(ServiceListing listing) {
        return templates.forListing(listing.getCategoryCode().name(), listing.getTemplateVersion());
    }
}
