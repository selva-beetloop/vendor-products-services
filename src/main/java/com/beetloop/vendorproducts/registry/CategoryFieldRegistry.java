package com.beetloop.vendorproducts.registry;

import com.beetloop.vendorproducts.domain.ProductCategory;
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
 * Loads {@code category-schemas.json} and answers "which fields belong to this
 * category / type card / role card, and which of them are required".
 *
 * <p>This is the single source of truth for the category-specific parts of the
 * wizard. Adding a field to the UI means adding one line here — no entity or
 * migration change — which is why the highly-variable Step 1 / Step 2 sections
 * are stored as JSON rather than columns.
 */
@Component
public class CategoryFieldRegistry {

    private static final Logger log = LoggerFactory.getLogger(CategoryFieldRegistry.class);

    private final ObjectMapper objectMapper;
    private CategorySchema.Root root;

    public CategoryFieldRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void load() {
        try (InputStream in = new ClassPathResource("category-schemas.json").getInputStream()) {
            this.root = objectMapper.readValue(in, CategorySchema.Root.class);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load category-schemas.json", e);
        }
        int fieldCount = 0;
        for (ProductCategory category : ProductCategory.values()) {
            fieldCount += identityBaseFields(category).size();
            for (String type : identityTypeIds(category)) {
                fieldCount += identityTypeFields(category, type).size();
            }
            fieldCount += roleFields(category, null).size();
        }
        log.info("Category field registry loaded: {} categories, ~{} declared fields",
                root.categories().size(), fieldCount);
    }

    public CategorySchema.Root raw() {
        return root;
    }

    public CategorySchema.Category category(ProductCategory category) {
        CategorySchema.Category definition = root.categories().get(category.getId());
        if (definition == null) {
            throw new IllegalStateException("No schema declared for category " + category.getId());
        }
        return definition;
    }

    // ---- Step 1: identity ----

    public List<FieldDefinition> identityBaseFields(ProductCategory category) {
        CategorySchema.Identity identity = category(category).identity();
        return identity == null || identity.baseFields() == null ? List.of() : identity.baseFields();
    }

    public List<String> identityTypeIds(ProductCategory category) {
        CategorySchema.Identity identity = category(category).identity();
        if (identity == null || identity.types() == null) {
            return List.of();
        }
        return new ArrayList<>(identity.types().keySet());
    }

    public boolean hasTypeSelector(ProductCategory category) {
        return !identityTypeIds(category).isEmpty();
    }

    public List<FieldDefinition> identityTypeFields(ProductCategory category, String typeId) {
        CategorySchema.Identity identity = category(category).identity();
        if (identity == null || identity.types() == null || typeId == null) {
            return List.of();
        }
        CategorySchema.Section section = identity.types().get(typeId);
        return section == null || section.fields() == null ? List.of() : section.fields();
    }

    /**
     * Fields that apply to a Step 1 submission.
     *
     * <p>The type card's form and the base form are <em>alternatives</em>, not
     * layers: in {@code MaterialIdentificationPage.tsx} the base "1.2 Product
     * Identity" block is the {@code else} branch, rendered only when the selected
     * type card has no dedicated form of its own (today that is {@code blend}).
     * So a type card with declared fields replaces the base set entirely;
     * otherwise the base set applies.
     */
    public List<FieldDefinition> identityFields(ProductCategory category, String typeId) {
        List<FieldDefinition> typeFields = identityTypeFields(category, typeId);
        return typeFields.isEmpty() ? new ArrayList<>(identityBaseFields(category)) : new ArrayList<>(typeFields);
    }

    // ---- Step 2: role ----

    public List<String> roleIds(ProductCategory category) {
        Map<String, CategorySchema.Section> roles = category(category).roles();
        return roles == null ? List.of() : new ArrayList<>(roles.keySet());
    }

    public boolean isKnownRole(ProductCategory category, String roleId) {
        Map<String, CategorySchema.Section> roles = category(category).roles();
        return roles != null && roles.containsKey(roleId);
    }

    /**
     * Fields that apply to a Step 2 submission for the given role: the category's
     * shared role fields plus the role card's own fields.
     */
    public List<FieldDefinition> roleFields(ProductCategory category, String roleId) {
        CategorySchema.Category definition = category(category);
        List<FieldDefinition> all = new ArrayList<>();
        if (definition.roleSharedFields() != null) {
            all.addAll(definition.roleSharedFields());
        }
        if (roleId != null && definition.roles() != null) {
            CategorySchema.Section section = definition.roles().get(roleId);
            if (section != null && section.fields() != null) {
                all.addAll(section.fields());
            }
        }
        return all;
    }

    // ---- Step 3: variants ----

    public List<FieldDefinition> variantDetailFields(ProductCategory category) {
        CategorySchema.Variant variant = category(category).variant();
        return variant == null || variant.detailFields() == null ? List.of() : variant.detailFields();
    }

    public List<FieldDefinition> variantExtraFields(ProductCategory category) {
        CategorySchema.Variant variant = category(category).variant();
        return variant == null || variant.extraFields() == null ? List.of() : variant.extraFields();
    }

    /** Category metadata for the "List a Product" card grid + wizard headers. */
    public Map<String, Object> categoryCatalog() {
        Map<String, Object> out = new LinkedHashMap<>();
        for (ProductCategory category : ProductCategory.values()) {
            out.put(category.getId(), category(category));
        }
        return Collections.unmodifiableMap(out);
    }
}
