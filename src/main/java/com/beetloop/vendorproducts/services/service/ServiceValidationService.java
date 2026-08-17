package com.beetloop.vendorproducts.services.service;

import com.beetloop.vendorproducts.exception.ValidationException;
import com.beetloop.vendorproducts.services.domain.ServiceCategory;
import com.beetloop.vendorproducts.services.domain.ServiceDocument;
import com.beetloop.vendorproducts.services.dto.ServiceDtos;
import com.beetloop.vendorproducts.services.registry.ServiceFieldRegistry;
import com.beetloop.vendorproducts.services.registry.ServiceSchema;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Server-side mirror of the Services wizard's own validation.
 *
 * <p>Required-field rules come from {@code service-schemas.json}, which was
 * populated from the components' {@code validate()} blocks and {@code required}
 * props, so a payload the UI would refuse is refused here too, with the message
 * keyed by the same field name the form state uses.
 *
 * <p>Option lists are deliberately <em>not</em> enforced — the same lesson the
 * products module learned: the wizards' select controls keep a pre-filled value
 * that is not in the dropdown list, and master-catalog rows legitimately seed
 * such values. Enforcing the list rejects payloads the UI considers valid on a
 * screen showing no error. The lists are still published for rendering.
 */
@Service
public class ServiceValidationService {

    private final ServiceFieldRegistry registry;

    public ServiceValidationService(ServiceFieldRegistry registry) {
        this.registry = registry;
    }

    /** Validates one wizard stage payload. */
    public void validateStage(ServiceCategory category, String stageKey,
                              Map<String, Object> data, boolean draft) {
        ValidationException.Builder errors = new ValidationException.Builder();

        if (!registry.isKnownStage(category, stageKey)) {
            errors.add("stageKey", "Unknown stage '" + stageKey + "' for " + category.getLabel()
                    + ". Expected one of: " + String.join(", ", registry.stageKeys(category)), stageKey);
            errors.throwIfAny("Stage is not valid for this category");
        }

        List<ServiceSchema.Field> fields = registry.stageFields(category, stageKey);
        // Unknown keys are NOT rejected on stage payloads — see the note below.
        if (!draft) {
            requireAll(data, fields, errors);
        }
        errors.throwIfAny("Stage '" + stageKey + "' is incomplete");
    }

    /**
     * Validates a document against the field set for its kind.
     *
     * <p>Also rejects a kind the category does not offer at all — Contract
     * Manufacturer has no accreditation modal, and Consultancy offers none of the
     * three, so accepting one silently would store a record the UI can never show.
     */
    public void validateDocument(ServiceCategory category, ServiceDtos.DocumentRequest request, boolean draft) {
        ValidationException.Builder errors = new ValidationException.Builder();

        ServiceDocument.Kind kind;
        try {
            kind = ServiceDocument.Kind.from(request.kind());
        } catch (IllegalArgumentException e) {
            errors.add("kind", e.getMessage(), request.kind());
            errors.throwIfAny("Document kind is not valid");
            return;
        }

        if (!registry.supportsDocumentKind(category, kind)) {
            errors.add("kind", category.getLabel() + " does not offer "
                    + kind.name().toLowerCase().replace('_', ' ') + " documents", request.kind());
            errors.throwIfAny("Document kind is not available for this category");
        }

        List<ServiceSchema.Field> fields = registry.documentFields(category, kind);
        rejectUnknownKeys(request.dataOrEmpty(), fields, "document", errors);
        if (!draft) {
            requireAll(request.dataOrEmpty(), fields, errors);
        }
        errors.throwIfAny("Document is incomplete");
    }

    /** Gate for the overall save and for Submit for QC. */
    public void validateCompleteBatch(ServiceCategory category,
                                      Map<String, Object> batchStages,
                                      int itemCount) {
        ValidationException.Builder errors = new ValidationException.Builder();
        if (itemCount == 0) {
            errors.add("items", "Add at least one service before submitting");
        }
        errors.throwIfAny("Service batch is not ready for submission");
    }

    // ---- helpers ----

    private void requireAll(Map<String, Object> data, List<ServiceSchema.Field> fields,
                            ValidationException.Builder errors) {
        for (ServiceSchema.Field field : fields) {
            if (!field.required()) {
                continue;
            }
            if (isEmpty(data == null ? null : data.get(field.name()))) {
                errors.add(field.name(), field.requiredMessage());
            }
        }
    }

    /**
     * Why stage payloads do not reject unknown keys.
     *
     * <p>The stage schemas were derived automatically from the components, so
     * alongside real inputs they document computed, derived and preview-only
     * entries that never appear in a payload. The wizards also keep their state
     * in its own natural shape — Lab Testing nests everything under
     * {@code form.pricing}, {@code form.capabilities} and so on — whereas the
     * registry lists the leaf names flat. Rejecting anything undeclared would
     * therefore refuse the wizard's own state and force a brittle flattening
     * contract on the client.
     *
     * <p>The check is kept for {@link #validateDocument}, where the field sets are
     * small, hand-checkable and genuinely the contract. Same judgement as the
     * option lists: enforce what is a contract, publish the rest as documentation.
     */

    /** Catches a frontend sending a field the backend would otherwise silently drop. */
    private void rejectUnknownKeys(Map<String, Object> data, List<ServiceSchema.Field> fields,
                                   String section, ValidationException.Builder errors) {
        if (data == null || data.isEmpty() || fields.isEmpty()) {
            return;
        }
        // Match on rootKey as well as the full name: the registry documents nested
        // paths (supportingAssets[].title) while the client sends the parent object.
        Set<String> known = new HashSet<>();
        for (ServiceSchema.Field field : fields) {
            known.add(field.name());
            String root = field.resolveRootKey();
            if (root != null && !root.isBlank()) {
                known.add(root);
            }
        }
        for (String key : data.keySet()) {
            if (!known.contains(key)) {
                errors.add(section + "." + key,
                        "Unknown field '" + key + "' — not declared in the service schema");
            }
        }
    }

    private boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String s) {
            return s.isBlank();
        }
        if (value instanceof Collection<?> c) {
            return c.isEmpty();
        }
        if (value instanceof Map<?, ?> m) {
            return m.isEmpty();
        }
        return false;
    }
}
