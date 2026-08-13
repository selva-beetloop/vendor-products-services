package com.beetloop.catalog.servicelisting;

import com.beetloop.catalog.servicelisting.dto.ServiceDtos;
import com.beetloop.catalog.servicelisting.model.ServiceConfiguration;
import com.beetloop.catalog.servicelisting.model.ServiceListing;
import com.beetloop.catalog.template.model.FormTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ServiceMapper {

    private ServiceMapper() {
    }

    public static ServiceDtos.ServiceResponse toResponse(ServiceListing listing, FormTemplate template) {
        return new ServiceDtos.ServiceResponse(
                listing.getId(),
                listing.getCode(),
                listing.getCategoryCode(),
                template.label(),
                listing.getCategoryId(),
                listing.getEntryPath(),
                listing.getTemplateVersion(),
                listing.getCurrentStep(),
                listing.getCompletedSteps(),
                stepLabels(template),
                template.childSectionKeys(),
                listing.getData(),
                listing.getConfigurations().stream()
                        .map(c -> toConfigurationResponse(c, template)).toList(),
                listing.getDerived(),
                listing.getQcStatus(),
                listing.getLifecycle(),
                listing.versionOrZero(),
                ServiceGuard.etag(listing),
                listing.getCreatedAt(),
                listing.getUpdatedAt());
    }

    /** The outer step labels vary per category - Compliance vs Accreditations, Publish vs Submit. */
    public static Map<String, String> stepLabels(FormTemplate template) {
        Map<String, String> labels = new LinkedHashMap<>();
        if (template.steps() != null) {
            template.steps().forEach(step -> labels.put(step.key(), step.label()));
        }
        return labels;
    }

    public static ServiceDtos.ConfigurationResponse toConfigurationResponse(
            ServiceConfiguration configuration, FormTemplate template) {
        return new ServiceDtos.ConfigurationResponse(
                configuration.getConfigurationId(),
                configuration.getSelectionId(),
                configuration.getMasterServiceId(),
                configuration.getRequestCode(),
                configuration.getName(),
                configuration.getServiceType(),
                configuration.getSource(),
                template.childSectionKeys(),
                configuration.getSections(),
                configuration.getConfigurationStatus(),
                configuration.getConfiguredAt(),
                configuration.getCompletionPercent());
    }

    public static List<String> sectionKeys(FormTemplate template) {
        return template.childSectionKeys();
    }
}
