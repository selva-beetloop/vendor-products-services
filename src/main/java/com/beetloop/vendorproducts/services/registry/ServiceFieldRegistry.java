package com.beetloop.vendorproducts.services.registry;

import com.beetloop.vendorproducts.services.domain.ServiceCategory;
import com.beetloop.vendorproducts.services.domain.ServiceDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads {@code service-schemas.json} and answers "which fields belong to this
 * category / stage / document kind, and which of them are required".
 *
 * <p>Single source of truth for the category-specific parts of the Services
 * wizard, exactly as {@code CategoryFieldRegistry} is for products. Adding a
 * field to the UI means one line here, with no entity or migration change.
 */
@Component
public class ServiceFieldRegistry {

    private static final Logger log = LoggerFactory.getLogger(ServiceFieldRegistry.class);

    private final ObjectMapper objectMapper;
    private ServiceSchema.Root root;

    public ServiceFieldRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void load() {
        try (InputStream in = new ClassPathResource("service-schemas.json").getInputStream()) {
            this.root = objectMapper.readValue(in, ServiceSchema.Root.class);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load service-schemas.json", e);
        }
        int fields = 0;
        int stages = 0;
        for (ServiceCategory category : ServiceCategory.values()) {
            ServiceSchema.Category definition = category(category);
            stages += definition.stages() == null ? 0 : definition.stages().size();
            fields += definition.fieldCount();
        }
        log.info("Service field registry loaded: {} categories, {} stages, {} declared fields",
                root.categories().size(), stages, fields);
    }

    public ServiceSchema.Root raw() {
        return root;
    }

    public ServiceSchema.Category category(ServiceCategory category) {
        ServiceSchema.Category definition = root.categories().get(category.getId());
        if (definition == null) {
            throw new IllegalStateException("No schema declared for service category " + category.getId());
        }
        return definition;
    }

    public List<String> stageKeys(ServiceCategory category) {
        List<ServiceSchema.Stage> stages = category(category).stages();
        if (stages == null) {
            return List.of();
        }
        return stages.stream().map(ServiceSchema.Stage::key).toList();
    }

    public boolean isKnownStage(ServiceCategory category, String stageKey) {
        return stageKeys(category).contains(stageKey);
    }

    /** Every field declared for one stage, flattened across its sections. */
    public List<ServiceSchema.Field> stageFields(ServiceCategory category, String stageKey) {
        List<ServiceSchema.Stage> stages = category(category).stages();
        if (stages == null || stageKey == null) {
            return List.of();
        }
        List<ServiceSchema.Field> out = new ArrayList<>();
        for (ServiceSchema.Stage stage : stages) {
            if (!stageKey.equals(stage.key())) {
                continue;
            }
            for (ServiceSchema.Section section : stage.sections()) {
                if (section.fields() != null) {
                    out.addAll(section.fields());
                }
            }
        }
        return out;
    }

    /** Fields for one document modal, or empty when the category does not offer that kind. */
    public List<ServiceSchema.Field> documentFields(ServiceCategory category, ServiceDocument.Kind kind) {
        ServiceSchema.Accreditations accreditations = category(category).accreditations();
        if (accreditations == null) {
            return List.of();
        }
        List<ServiceSchema.Field> fields = switch (kind) {
            case ACCREDITATION -> accreditations.accreditation();
            case CERTIFICATION -> accreditations.certification();
            case SUPPORT_DOC -> accreditations.supportDoc();
        };
        return fields == null ? List.of() : fields;
    }

    /** True when the category actually offers this document kind in its UI. */
    public boolean supportsDocumentKind(ServiceCategory category, ServiceDocument.Kind kind) {
        return !documentFields(category, kind).isEmpty();
    }

    /** Category metadata for the "List a Service" chooser and the wizard headers. */
    public Map<String, Object> categoryCatalog() {
        Map<String, Object> out = new LinkedHashMap<>();
        for (ServiceCategory category : ServiceCategory.values()) {
            out.put(category.getId(), category(category));
        }
        return Collections.unmodifiableMap(out);
    }
}
