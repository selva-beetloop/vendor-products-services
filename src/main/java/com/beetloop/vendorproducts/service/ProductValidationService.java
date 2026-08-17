package com.beetloop.vendorproducts.service;

import com.beetloop.vendorproducts.domain.ProductCategory;
import com.beetloop.vendorproducts.domain.VendorProduct;
import com.beetloop.vendorproducts.dto.VariantRequest;
import com.beetloop.vendorproducts.exception.ValidationException;
import com.beetloop.vendorproducts.registry.CategoryFieldRegistry;
import com.beetloop.vendorproducts.registry.FieldDefinition;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Server-side mirror of the frontend's {@code validate()} blocks.
 *
 * <p>Required-field rules and their messages come from
 * {@code category-schemas.json}, which was populated from those same blocks — so
 * a payload the UI would refuse is also refused here, with the identical message
 * keyed by the identical field name.
 */
@Service
public class ProductValidationService {

    private final CategoryFieldRegistry registry;

    public ProductValidationService(CategoryFieldRegistry registry) {
        this.registry = registry;
    }

    // ---- Step 1 ----

    public void validateIdentity(ProductCategory category, String identityType,
                                 Map<String, Object> data, boolean draft) {
        ValidationException.Builder errors = new ValidationException.Builder();

        if (registry.hasTypeSelector(category)) {
            List<String> knownTypes = registry.identityTypeIds(category);
            if (isBlank(identityType)) {
                if (!draft) {
                    errors.add("identityType", "Select a "
                            + registry.category(category).identity().typeSelectorLabel());
                }
            } else if (!knownTypes.contains(identityType)) {
                errors.add("identityType",
                        "Unknown type '" + identityType + "'. Expected one of: " + String.join(", ", knownTypes),
                        identityType);
            }
        }

        List<FieldDefinition> fields = registry.identityFields(category, identityType);
        rejectUnknownKeys(data, fields, "identity", errors);
        if (!draft) {
            requireAll(data, fields, errors);
        }
        validateOptions(data, fields, errors);

        errors.throwIfAny("Product identity is incomplete");
    }

    // ---- Step 2 ----

    public void validateRole(ProductCategory category, String roleId,
                             Map<String, Object> data, boolean draft) {
        ValidationException.Builder errors = new ValidationException.Builder();

        if (isBlank(roleId)) {
            errors.add("roleId", "Select your role");
        } else if (!registry.isKnownRole(category, roleId)) {
            errors.add("roleId", "Unknown role '" + roleId + "'. Expected one of: "
                    + String.join(", ", registry.roleIds(category)), roleId);
        }

        if (!errors.isEmpty()) {
            errors.throwIfAny("Your Role step is incomplete");
        }

        List<FieldDefinition> fields = registry.roleFields(category, roleId);
        rejectUnknownKeys(data, fields, "role", errors);
        if (!draft) {
            requireAll(data, fields, errors);
        }
        validateOptions(data, fields, errors);

        errors.throwIfAny("Your Role step is incomplete");
    }

    // ---- Step 3 ----

    public void validateVariant(ProductCategory category, VariantRequest request, int index, boolean draft) {
        ValidationException.Builder errors = new ValidationException.Builder();
        String prefix = index < 0 ? "" : "variants[" + index + "].";

        VariantRequest.VariantDetails details = request.variantDetails();
        if (details == null) {
            if (!draft) {
                errors.add(prefix + "variantDetails", "Variant details are required");
            }
        } else if (!draft && isBlank(details.name())) {
            errors.add(prefix + "variantDetails.name", "Variant Name is required");
        }

        // `variantDetails.extra` is deliberately an OPEN bag.
        //
        // It exists to carry whatever category-specific fields a variant row has
        // (capacity, powerRating, thicknessUm, barrierLevel, closureType, …), and
        // those differ per category and evolve with the UI. Rejecting undeclared
        // keys here would defeat its purpose and force the frontend to know the
        // backend's field list before it could send anything.
        //
        // The identity and role payloads are the opposite case — their field sets
        // ARE the contract, so unknown keys there are still rejected (see
        // validateIdentity / validateRole), which is where drift actually matters.

        // Technical specifications: a parameter row is either fully blank (ignored
        // on save, exactly like the UI's isSpecRowComplete check) or complete.
        VariantRequest.TechnicalSpecifications specs = request.technicalSpecifications();
        if (specs != null && !draft) {
            List<VariantRequest.SpecificationGroupDto> groups = specs.dataOrEmpty();
            for (int g = 0; g < groups.size(); g++) {
                VariantRequest.SpecificationGroupDto group = groups.get(g);
                if (isBlank(group.title())) {
                    errors.add(prefix + "technicalSpecifications.data[" + g + "].title",
                            "Specification Title is required");
                }
                List<VariantRequest.SpecificationParameterDto> rows = group.dataOrEmpty();
                for (int r = 0; r < rows.size(); r++) {
                    VariantRequest.SpecificationParameterDto row = rows.get(r);
                    if (isCompletelyBlank(row)) {
                        continue;
                    }
                    String rowPrefix = prefix + "technicalSpecifications.data[" + g + "].data[" + r + "].";
                    if (isBlank(row.parameterName())) {
                        errors.add(rowPrefix + "parameterName", "Parameter is required");
                    }
                    if (isBlank(row.specification())) {
                        errors.add(rowPrefix + "specification", "Specification is required");
                    }
                    if (isBlank(row.unit())) {
                        errors.add(rowPrefix + "unit", "Unit is required");
                    }
                    if (isBlank(row.testMethodOrStandard())) {
                        errors.add(rowPrefix + "testMethodOrStandard", "Test Method / Standard is required");
                    }
                    if (isBlank(row.requirementSource())) {
                        errors.add(rowPrefix + "requirementSource", "Requirement Source is required");
                    }
                }
            }
        }

        // Compliance documents: Name is the only field the modal's Add button gates on.
        VariantRequest.ComplianceCertifications compliance = request.complianceCertifications();
        if (compliance != null) {
            List<VariantRequest.ComplianceDocumentDto> documents = compliance.dataOrEmpty();
            for (int d = 0; d < documents.size(); d++) {
                if (isBlank(documents.get(d).name())) {
                    errors.add(prefix + "complianceCertifications.data[" + d + "].name",
                            "Name is required");
                }
            }
        }

        errors.throwIfAny(index < 0 ? "Variant is incomplete" : "Variant " + (index + 1) + " is incomplete");
    }

    // ---- Overall save / submit ----

    /**
     * Gate for {@code POST /products/{id}/save} (non-draft) and
     * {@code POST /products/{id}/submit}: all three top-level steps must be present.
     */
    public void validateCompleteProduct(VendorProduct product) {
        ValidationException.Builder errors = new ValidationException.Builder();

        if (product.getIdentityPayload() == null || product.getIdentityPayload().isEmpty()) {
            errors.add("productIdentity", "Complete the Product Identity step before submitting");
        }
        if (isBlank(product.getRoleId())) {
            errors.add("yourRole", "Complete the Your Role step before submitting");
        }
        if (product.getVariants().isEmpty()) {
            errors.add("variants", "Add at least one variant before submitting");
        }
        errors.throwIfAny("Product is not ready for submission");

        validateIdentity(product.getCategory(), product.getIdentityType(),
                product.getIdentityPayload(), false);
        validateRole(product.getCategory(), product.getRoleId(), product.getRolePayload(), false);
        validateExpiryDocuments(product);
    }

    /**
     * Expired compliance dates are hard errors — they block submit and publish.
     */
    public void validateExpiryDocuments(VendorProduct product) {
        ValidationException.Builder errors = new ValidationException.Builder();
        java.time.LocalDate today = java.time.LocalDate.now();
        int variantIndex = 0;
        for (var variant : product.getVariants()) {
            int docIndex = 0;
            for (var document : variant.getComplianceDocuments()) {
                String raw = document.getExpiryDate();
                if (raw == null || raw.isBlank()) {
                    docIndex++;
                    continue;
                }
                java.time.LocalDate expiry = parseDate(raw);
                if (expiry == null) {
                    errors.add("variants[" + variantIndex + "].complianceCertifications.data["
                                    + docIndex + "].expiryDate",
                            "Expiry date could not be parsed", raw);
                } else if (expiry.isBefore(today)) {
                    errors.add("variants[" + variantIndex + "].complianceCertifications.data["
                                    + docIndex + "].expiryDate",
                            "Certification is expired", raw);
                }
                docIndex++;
            }
            variantIndex++;
        }
        errors.throwIfAny("Expired or unparseable certifications block submit");
    }

    private java.time.LocalDate parseDate(String raw) {
        String value = raw.trim();
        java.time.format.DateTimeFormatter[] formats = {
                java.time.format.DateTimeFormatter.ISO_LOCAL_DATE,
                java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.ENGLISH),
                java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.ENGLISH),
                java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.ENGLISH),
                java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.ENGLISH),
        };
        for (java.time.format.DateTimeFormatter formatter : formats) {
            try {
                return java.time.LocalDate.parse(value, formatter);
            } catch (java.time.format.DateTimeParseException ignored) {
                // try next
            }
        }
        return null;
    }

    // ---- helpers ----

    private void requireAll(Map<String, Object> data, List<FieldDefinition> fields,
                            ValidationException.Builder errors) {
        for (FieldDefinition field : fields) {
            if (!field.required()) {
                continue;
            }
            if (isEmptyValue(data == null ? null : data.get(field.name()))) {
                errors.add(field.name(), field.requiredMessage());
            }
        }
    }

    /**
     * Rejects keys that are not declared for this category/type/role. Catches a
     * frontend sending a field the backend would otherwise silently drop.
     */
    private void rejectUnknownKeys(Map<String, Object> data, List<FieldDefinition> fields,
                                   String section, ValidationException.Builder errors) {
        if (data == null || data.isEmpty()) {
            return;
        }
        Set<String> known = new HashSet<>();
        for (FieldDefinition field : fields) {
            known.add(field.name());
        }
        for (String key : data.keySet()) {
            if (!known.contains(key)) {
                errors.add(section + "." + key,
                        "Unknown field '" + key + "' for this " + section + " — not declared in the "
                                + "category schema");
            }
        }
    }

    /**
     * Option lists are <em>advisory</em>, not a closed enum — deliberately not
     * enforced.
     *
     * <p>The wizard's own select controls keep a pre-filled value that is not in
     * the dropdown list rather than discarding it:
     *
     * <pre>
     * const choices = options.includes(value) ? options : value ? [value, ...options] : options;
     * </pre>
     *
     * and the master-catalog search data legitimately seeds values in a different
     * form from the dropdown's (e.g. {@code packagingSubCategory: "Bottles"} from
     * the search row versus {@code "Bottle"} in the option list). Rejecting those
     * would refuse payloads the UI considers valid and would block Save &amp;
     * Continue on a screen showing no error.
     *
     * <p>The lists are still published by {@code GET /catalog/categories} so the
     * frontend can render the dropdowns, and required-field rules — the checks
     * the UI actually gates on — are still enforced.
     */
    private void validateOptions(Map<String, Object> data, List<FieldDefinition> fields,
                                 ValidationException.Builder errors) {
        // Intentionally no-op. See the javadoc above before adding enforcement.
    }

    private boolean isCompletelyBlank(VariantRequest.SpecificationParameterDto row) {
        return isBlank(row.parameterName()) && isBlank(row.specification()) && isBlank(row.unit())
                && isBlank(row.testMethodOrStandard()) && isBlank(row.requirementSource());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isEmptyValue(Object value) {
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
