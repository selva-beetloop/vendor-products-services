package com.beetloop.catalog.template;

import com.beetloop.catalog.shared.error.ApiException;
import com.beetloop.catalog.shared.error.ErrorCode;
import com.beetloop.catalog.template.model.FormTemplate;
import com.beetloop.catalog.template.model.TemplateStatus;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * A draft pins the templateVersion it was authored against and keeps validating against it until it
 * is submitted. Publishing a new ACTIVE version affects new drafts only - otherwise a template edit
 * would retroactively invalidate every in-flight draft, which with nine cards and seven roles per
 * category would happen on nearly every product iteration.
 */
@Service
public class TemplateService {

    private final FormTemplateRepository repository;

    public TemplateService(FormTemplateRepository repository) {
        this.repository = repository;
    }

    @Cacheable(cacheNames = "formTemplateActive", key = "#categoryCode")
    public FormTemplate active(String categoryCode) {
        return repository.findFirstByCategoryCodeAndStatusOrderByVersionDesc(categoryCode, TemplateStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.UNKNOWN_TEMPLATE,
                        "No ACTIVE form template exists for category %s.".formatted(categoryCode)));
    }

    @Cacheable(cacheNames = "formTemplateVersion", key = "#categoryCode + ':' + #version")
    public FormTemplate version(String categoryCode, int version) {
        return repository.findByCategoryCodeAndVersion(categoryCode, version)
                .orElseThrow(() -> new ApiException(ErrorCode.UNKNOWN_TEMPLATE,
                        "Form template %s v%d does not exist.".formatted(categoryCode, version)));
    }

    /** Resolve the template a given draft is pinned to. */
    public FormTemplate forListing(String categoryCode, Integer pinnedVersion) {
        return pinnedVersion == null ? active(categoryCode) : version(categoryCode, pinnedVersion);
    }

    public FormTemplate.StepSchema requireStep(FormTemplate template, String stepKey) {
        return template.step(stepKey).orElseThrow(() ->
                new ApiException(ErrorCode.UNKNOWN_STEP,
                        "'%s' is not a step of %s template v%d."
                                .formatted(stepKey, template.categoryCode(), template.version()))
                        .with("validSteps", template.stepKeys()));
    }

    public FormTemplate.SectionSchema requireChildSection(FormTemplate template, String sectionKey) {
        return template.childSection(sectionKey).orElseThrow(() ->
                new ApiException(ErrorCode.UNKNOWN_SECTION,
                        "'%s' is not a %s section of %s."
                                .formatted(sectionKey,
                                        template.childCollection() == com.beetloop.catalog.template.model
                                                .ChildCollection.VARIANTS ? "variant" : "configuration",
                                        template.categoryCode()))
                        .with("validSections", template.childSectionKeys()));
    }
}
