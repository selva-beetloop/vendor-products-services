package com.beetloop.catalog.template;

import com.beetloop.catalog.config.CatalogProperties;
import com.beetloop.catalog.shared.error.FieldError;
import com.beetloop.catalog.shared.error.RejectedField;
import com.beetloop.catalog.shared.error.Warning;
import com.beetloop.catalog.shared.util.DateNormalizer;
import com.beetloop.catalog.shared.util.Maps;
import com.beetloop.catalog.template.model.FieldAccess;
import com.beetloop.catalog.template.model.FieldType;
import com.beetloop.catalog.template.model.FormTemplate;
import com.beetloop.catalog.template.model.FormTemplate.DiscriminatorOption;
import com.beetloop.catalog.template.model.FormTemplate.FieldSchema;
import com.beetloop.catalog.template.model.FormTemplate.SectionSchema;
import com.beetloop.catalog.template.model.FormTemplate.StepSchema;
import com.beetloop.catalog.template.model.FormTemplate.RuleSpec;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Walks a step or section against its form template.
 *
 * Two behaviours are worth calling out because they are the ones integrations get wrong:
 *
 *  1. A write to a read-only field does NOT fail the request. It is reported in rejectedFields[]
 *     and dropped. A byte-identical echo of a LINKED/DERIVED value is dropped SILENTLY, because the
 *     client legitimately round-trips the whole object it was handed.
 *  2. Required fields are enforced only in SUBMIT mode. Save must never reject a half-filled step.
 */
@Component
public class ValidationEngine {

    private final VocabularyPort vocabulary;
    private final DateNormalizer dateNormalizer;
    private final CatalogProperties properties;

    public ValidationEngine(VocabularyPort vocabulary, DateNormalizer dateNormalizer,
                            CatalogProperties properties) {
        this.vocabulary = vocabulary;
        this.dateNormalizer = dateNormalizer;
        this.properties = properties;
    }

    // ------------------------------------------------------------------ public entry points

    /** Validate one wizard step (identity / role / variants / review, or the four service steps). */
    public ValidationResult validateStep(FormTemplate template, StepSchema step,
                                         Map<String, Object> incoming, Map<String, Object> existing,
                                         ValidationMode mode) {
        return validateStep(template, step, incoming, existing, mode, true);
    }

    /**
     * @param carryOverOtherCards true for a step-wise save - a vendor switching cards and switching
     *                            back must not lose the first card's data. False for the overall
     *                            save, which is a full replace by contract.
     */
    public ValidationResult validateStep(FormTemplate template, StepSchema step,
                                         Map<String, Object> incoming, Map<String, Object> existing,
                                         ValidationMode mode, boolean carryOverOtherCards) {
        Ctx ctx = new Ctx(mode, step.key(), "data." + step.dataKey());
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Object> in = Maps.orEmpty(incoming);
        Map<String, Object> old = Maps.orEmpty(existing);
        Set<String> declared = new HashSet<>();

        FormTemplate.Discriminator disc = step.discriminator();
        if (disc != null) {
            declared.add(disc.field());
            String selected = resolveDiscriminator(disc, in, old);
            if (selected != null) {
                out.put(disc.field(), selected);
            }
            if (selected == null) {
                if (mode == ValidationMode.SUBMIT) {
                    ctx.error(ctx.path(disc.field()), disc.label(), "FIELD_REQUIRED",
                            "%s is required".formatted(nz(disc.label(), disc.field())), null);
                }
            } else if (!disc.codes().contains(selected)) {
                ctx.error(ctx.path(disc.field()), disc.label(), "VALUE_NOT_IN_VOCABULARY",
                        "'%s' is not a valid %s.".formatted(selected, nz(disc.label(), disc.field())),
                        selected);
            } else {
                DiscriminatorOption option = disc.option(selected).orElseThrow();
                String storeUnder = option.storeUnder();
                declared.add(storeUnder);
                // Every option's sub-object is a declared key: the vendor may switch cards freely and
                // the other cards' data must survive a step-wise save.
                disc.options().forEach(o -> declared.add(o.storeUnder()));

                Map<String, Object> subIn = Maps.orEmpty(Maps.mapAt(in, storeUnder));
                Map<String, Object> subOld = Maps.orEmpty(Maps.mapAt(old, storeUnder));
                if (subIn.isEmpty() && mode == ValidationMode.SUBMIT) {
                    ctx.error(ctx.path(storeUnder), option.label(), "DISCRIMINATOR_SUBFORM_MISSING",
                            "%s is selected but no details were provided.".formatted(option.label()), null);
                }
                Map<String, Object> subOut = new LinkedHashMap<>();
                ctx.push(storeUnder);
                walkSections(option.sections(), subIn, subOld, subOut, ctx);
                ctx.pop();
                out.put(storeUnder, subOut);

                // Carry every other card's stored sub-object through untouched.
                for (DiscriminatorOption other : disc.options()) {
                    if (other.storeUnder().equals(storeUnder)) {
                        continue;
                    }
                    Object carried = in.containsKey(other.storeUnder())
                            ? in.get(other.storeUnder())
                            : (carryOverOtherCards ? old.get(other.storeUnder()) : null);
                    if (carried != null) {
                        out.put(other.storeUnder(), carried);
                    }
                }
            }
        }

        walkSections(step.sections(), in, old, out, ctx);
        collectDeclared(step.sections(), declared);
        rejectUnknown(in, declared, ctx);

        return ctx.result(out);
    }

    /** Validate one variant stage or one service-configuration sub-step. */
    public ValidationResult validateSection(SectionSchema section, Map<String, Object> incoming,
                                            Map<String, Object> existing, ValidationMode mode,
                                            String stepKeyForErrors, String pathPrefix) {
        Ctx ctx = new Ctx(mode, stepKeyForErrors, pathPrefix);
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Object> in = Maps.orEmpty(incoming);
        Map<String, Object> old = Maps.orEmpty(existing);

        walkFields(section.fields(), in, old, out, ctx);
        walkSections(section.sections(), in, old, out, ctx);

        Set<String> declared = new HashSet<>();
        if (section.fields() != null) {
            section.fields().forEach(f -> declared.add(f.key()));
        }
        collectDeclared(section.sections(), declared);
        rejectUnknown(in, declared, ctx);

        return ctx.result(out);
    }

    /** Server-side completeness. The UI's own "Complete" badges are decorative and cannot be trusted. */
    public boolean isStepComplete(FormTemplate template, StepSchema step, Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return false;
        }
        return validateStep(template, step, data, data, ValidationMode.SUBMIT).valid();
    }

    public boolean isSectionComplete(SectionSchema section, Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return false;
        }
        return validateSection(section, data, data, ValidationMode.SUBMIT, null, "data").valid();
    }

    // ------------------------------------------------------------------ walking

    private String resolveDiscriminator(FormTemplate.Discriminator disc, Map<String, Object> in,
                                        Map<String, Object> old) {
        Object value = in.get(disc.field());
        if (Maps.isBlank(value)) {
            value = old.get(disc.field());
        }
        if (Maps.isBlank(value)) {
            value = disc.defaultValue();
        }
        return Maps.isBlank(value) ? null : String.valueOf(value);
    }

    private void walkSections(List<SectionSchema> sections, Map<String, Object> in,
                              Map<String, Object> old, Map<String, Object> out, Ctx ctx) {
        if (sections == null) {
            return;
        }
        for (SectionSchema section : sections) {
            String key = section.dataKey() != null ? section.dataKey() : section.key();
            if (key == null || key.isBlank()) {
                // Fields sit at the current level rather than nesting.
                walkFields(section.fields(), in, old, out, ctx);
                walkSections(section.sections(), in, old, out, ctx);
                continue;
            }
            Map<String, Object> subIn = Maps.orEmpty(Maps.mapAt(in, key));
            Map<String, Object> subOld = Maps.orEmpty(Maps.mapAt(old, key));
            Map<String, Object> subOut = new LinkedHashMap<>();
            ctx.push(key);
            walkFields(section.fields(), subIn, subOld, subOut, ctx);
            walkSections(section.sections(), subIn, subOld, subOut, ctx);
            ctx.pop();
            if (!subOut.isEmpty() || in.containsKey(key)) {
                out.put(key, subOut);
            }
        }
    }

    private void walkFields(List<FieldSchema> fields, Map<String, Object> in, Map<String, Object> old,
                            Map<String, Object> out, Ctx ctx) {
        if (fields == null) {
            return;
        }
        for (FieldSchema field : fields) {
            validateField(field, in, old, out, ctx);
        }
        for (FieldSchema field : fields) {
            evaluateRules(field, in, out, ctx);
        }
    }

    private void validateField(FieldSchema field, Map<String, Object> in, Map<String, Object> old,
                               Map<String, Object> out, Ctx ctx) {
        String key = field.key();
        String path = ctx.path(key);

        if (!field.writable()) {
            handleReadOnly(field, in, old, out, ctx, path);
            return;
        }

        Object raw = in.get(key);
        if (Maps.isBlank(raw)) {
            if (field.required() && ctx.mode == ValidationMode.SUBMIT) {
                ctx.error(path, field.label(), "FIELD_REQUIRED",
                        "%s is required".formatted(nz(field.label(), key)), null);
            }
            if (in.containsKey(key)) {
                out.put(key, raw);
            }
            return;
        }

        Object coerced = coerce(field, raw, path, ctx);
        if (coerced == null) {
            return;
        }

        switch (field.type()) {
            case OBJECT -> {
                Map<String, Object> subOut = new LinkedHashMap<>();
                ctx.push(key);
                walkFields(field.fields(), Maps.orEmpty(Maps.asMap(coerced)),
                        Maps.orEmpty(Maps.mapAt(old, key)), subOut, ctx);
                ctx.pop();
                out.put(key, subOut);
                return;
            }
            case REPEATABLE -> {
                List<Map<String, Object>> rows = Maps.asMapList(coerced);
                checkArrayBounds(field, rows.size(), path, ctx);
                List<Object> rowsOut = new ArrayList<>();
                int index = 0;
                for (Map<String, Object> row : rows) {
                    Map<String, Object> rowOut = new LinkedHashMap<>();
                    ctx.push(key + "[" + index + "]");
                    walkFields(field.fields(), row, Map.of(), rowOut, ctx);
                    ctx.pop();
                    // Preserve row-level keys the template does not declare (ids minted server-side).
                    row.forEach(rowOut::putIfAbsent);
                    rowsOut.add(rowOut);
                    index++;
                }
                out.put(key, rowsOut);
                return;
            }
            default -> {
                // fall through to scalar checks
            }
        }

        checkScalar(field, coerced, path, ctx);
        out.put(key, coerced);
    }

    private void handleReadOnly(FieldSchema field, Map<String, Object> in, Map<String, Object> old,
                                Map<String, Object> out, Ctx ctx, String path) {
        String key = field.key();
        Object storedValue = old.get(key);
        if (in.containsKey(key)) {
            Object submitted = in.get(key);
            if (!Objects.equals(normalizeForCompare(submitted), normalizeForCompare(storedValue))) {
                ctx.rejected(new RejectedField(path, reasonFor(field.accessOrDefault()),
                        messageFor(field), submitted));
            }
            // else: byte-identical echo of a value we handed the client - drop it silently.
        }
        if (storedValue != null) {
            out.put(key, storedValue);
        }
    }

    private String reasonFor(FieldAccess access) {
        return switch (access) {
            case AUTO -> RejectedField.READ_ONLY_AUTO;
            case LINKED -> RejectedField.READ_ONLY_LINKED;
            case DERIVED -> RejectedField.DERIVED_FIELD;
            case BILLING_GATED -> RejectedField.BILLING_GATED_FIELD;
            case SYSTEM -> RejectedField.SYSTEM_FIELD;
            case EDITABLE -> RejectedField.SYSTEM_FIELD;
        };
    }

    private String messageFor(FieldSchema field) {
        return switch (field.accessOrDefault()) {
            case AUTO -> "%s is auto-generated and cannot be written.".formatted(nz(field.label(), field.key()));
            case LINKED -> "%s is projected from the linked record and cannot be written."
                    .formatted(nz(field.label(), field.key()));
            case DERIVED -> "%s is computed and cannot be written.".formatted(nz(field.label(), field.key()));
            case BILLING_GATED -> "Paid tiers are activated through the promotion endpoint, not a save.";
            default -> "%s cannot be written.".formatted(nz(field.label(), field.key()));
        };
    }

    // ------------------------------------------------------------------ scalar checks

    private Object coerce(FieldSchema field, Object raw, String path, Ctx ctx) {
        try {
            return switch (field.type()) {
                case NUMBER -> toNumber(raw);
                case TOGGLE -> toBoolean(raw);
                case CHIPS, MULTI_SELECT, CHECKBOX_GROUP, COUNTRY_MULTI, FILE_MULTI -> {
                    List<Object> list = Maps.asList(raw);
                    if (list == null) {
                        throw new IllegalArgumentException("expected an array");
                    }
                    yield list;
                }
                case REPEATABLE, SPEC_GROUPS -> {
                    if (Maps.asList(raw) == null) {
                        throw new IllegalArgumentException("expected an array");
                    }
                    yield raw;
                }
                case OBJECT, MEASURE, DIMENSIONS, FILE_SINGLE -> {
                    if (Maps.asMap(raw) == null) {
                        throw new IllegalArgumentException("expected an object");
                    }
                    yield raw;
                }
                case DATE_TEXT, DATE -> raw;
                default -> raw instanceof String ? raw : String.valueOf(raw);
            };
        } catch (RuntimeException e) {
            ctx.error(path, field.label(), "TYPE_MISMATCH",
                    "%s could not be read as %s.".formatted(nz(field.label(), field.key()),
                            field.type().name().toLowerCase().replace('_', ' ')), raw);
            return null;
        }
    }

    private void checkScalar(FieldSchema field, Object value, String path, Ctx ctx) {
        switch (field.type()) {
            case TEXT, TEXTAREA, AUTO_TEXT -> checkText(field, String.valueOf(value), path, ctx);
            case NUMBER -> checkNumber(field, (BigDecimal) value, path, ctx);
            case SELECT, RADIO, COUNTRY -> checkVocabulary(field, String.valueOf(value), path, ctx, null);
            case MULTI_SELECT, CHECKBOX_GROUP, COUNTRY_MULTI -> {
                List<Object> list = Maps.asList(value);
                checkArrayBounds(field, list.size(), path, ctx);
                for (Object item : list) {
                    checkVocabulary(field, String.valueOf(item), path, ctx, item);
                }
            }
            case CHIPS -> {
                List<Object> list = Maps.asList(value);
                checkArrayBounds(field, list.size(), path, ctx);
            }
            case FILE_MULTI -> checkArrayBounds(field, Maps.asList(value).size(), path, ctx);
            case MEASURE -> checkMeasure(field, Maps.asMap(value), path, ctx);
            case DATE_TEXT -> {
                if (dateNormalizer.normalize(value) == null) {
                    ctx.error(path, field.label(), "UNPARSEABLE_DATE",
                            "Accepted formats: %s.".formatted(String.join(", ", DateNormalizer.ACCEPTED_FORMATS)),
                            value);
                }
            }
            default -> {
                // no scalar constraints
            }
        }
    }

    private void checkText(FieldSchema field, String value, String path, Ctx ctx) {
        if (field.maxLength() != null && value.length() > field.maxLength()) {
            ctx.error(path, field.label(), "MAX_LENGTH_EXCEEDED",
                    "%s must be %d characters or fewer.".formatted(nz(field.label(), field.key()),
                            field.maxLength()), value);
            ctx.replaceLastMeta(Map.of("max", field.maxLength(), "received", value.length()));
        }
        if (field.pattern() != null && !Pattern.compile(field.pattern()).matcher(value).matches()) {
            ctx.error(path, field.label(), "PATTERN_MISMATCH",
                    "%s is not in the expected format.".formatted(nz(field.label(), field.key())), value);
        }
    }

    private void checkNumber(FieldSchema field, BigDecimal value, String path, Ctx ctx) {
        if (field.min() != null && value.doubleValue() < field.min()) {
            ctx.error(path, field.label(), "MIN_VALUE",
                    "%s must be at least %s.".formatted(nz(field.label(), field.key()), field.min()), value);
        }
        if (field.max() != null && value.doubleValue() > field.max()) {
            ctx.error(path, field.label(), "MAX_VALUE",
                    "%s must be at most %s.".formatted(nz(field.label(), field.key()), field.max()), value);
        }
    }

    private void checkVocabulary(FieldSchema field, String value, String path, Ctx ctx, Object rejected) {
        if (field.vocabularyCode() == null || value == null || value.isBlank()) {
            return;
        }
        if (vocabulary.contains(field.vocabularyCode(), null, value)) {
            return;
        }
        if (field.allowCustomValues() && vocabulary.isCustomValue(field.key(), value)) {
            return;
        }
        ctx.error(path, field.label(), "VALUE_NOT_IN_VOCABULARY",
                "'%s' is not a valid %s.".formatted(value, nz(field.label(), field.key())),
                rejected == null ? value : rejected);
    }

    private void checkMeasure(FieldSchema field, Map<String, Object> measure, String path, Ctx ctx) {
        if (measure == null) {
            return;
        }
        Object unit = measure.get("unit");
        if (Maps.isBlank(unit)) {
            return;
        }
        List<String> allowed = field.units();
        if (allowed != null && !allowed.isEmpty() && !allowed.contains(String.valueOf(unit))) {
            ctx.error(path + ".unit", field.label(), "UNIT_NOT_IN_VOCABULARY",
                    "'%s' is not a valid unit for this field. Allowed: %s."
                            .formatted(unit, String.join(", ", allowed)), unit);
            return;
        }
        if (field.unitVocabularyCode() != null
                && !vocabulary.contains(field.unitVocabularyCode(), null, String.valueOf(unit))) {
            ctx.error(path + ".unit", field.label(), "UNIT_NOT_IN_VOCABULARY",
                    "'%s' is not a valid unit for this field.".formatted(unit), unit);
        }
    }

    private void checkArrayBounds(FieldSchema field, int size, String path, Ctx ctx) {
        Integer max = field.maxItems();
        if (max != null && size > max) {
            ctx.error(path, field.label(), "ARRAY_CAP_EXCEEDED",
                    "%s accepts at most %d item%s.".formatted(nz(field.label(), field.key()), max,
                            max == 1 ? "" : "s"), size);
            ctx.replaceLastMeta(Map.of("max", max, "received", size));
        } else if (max != null && size >= Math.max(1, (int) Math.ceil(max * 0.9))) {
            ctx.warn(path, "APPROACHING_CAP",
                    "%d of a maximum %d used.".formatted(size, max));
        }
        Integer min = field.minItems();
        if (min != null && size < min && ctx.mode == ValidationMode.SUBMIT) {
            ctx.error(path, field.label(), "ARRAY_MIN_ITEMS",
                    "%s requires at least %d item%s.".formatted(nz(field.label(), field.key()), min,
                            min == 1 ? "" : "s"), size);
        }
    }

    // ------------------------------------------------------------------ rules

    private void evaluateRules(FieldSchema field, Map<String, Object> level, Map<String, Object> out,
                               Ctx ctx) {
        if (field.rules() == null || field.rules().isEmpty()) {
            return;
        }
        String path = ctx.path(field.key());
        for (RuleSpec rule : field.rules()) {
            Map<String, Object> p = rule.params() == null ? Map.of() : rule.params();
            switch (rule.type()) {
                case REQUIRED_WHEN -> {
                    String when = str(p.get("field"));
                    Object equals = p.get("equals");
                    Object actual = level.get(when);
                    boolean triggered = equals == null
                            ? !Maps.isBlank(actual)
                            : String.valueOf(equals).equals(String.valueOf(actual));
                    if (triggered && Maps.isBlank(level.get(field.key()))
                            && ctx.mode == ValidationMode.SUBMIT) {
                        ctx.errorConditional(path, field.label(), "FIELD_REQUIRED_WHEN",
                                nz(rule.message(), "%s is required.".formatted(nz(field.label(), field.key()))),
                                "%s == %s".formatted(when, equals));
                    }
                }
                case CONTIGUOUS_TIERS -> checkTiers(field, level, path, ctx, p, rule);
                case DATE_RANGE -> {
                    LocalDate from = dateOf(level.get(str(p.get("from"))));
                    LocalDate to = dateOf(level.get(str(p.get("to"))));
                    if (from != null && to != null && !from.isBefore(to)) {
                        ctx.error(path, field.label(), "DATE_RANGE_INVERTED",
                                nz(rule.message(), "The end date must be after the start date."), to);
                    }
                }
                case DATE_NOT_IN_PAST -> {
                    LocalDate value = dateOf(level.get(field.key()));
                    if (value != null && value.isBefore(LocalDate.now())
                            && ctx.mode == ValidationMode.SUBMIT) {
                        ctx.error(path, field.label(), "DATE_IN_PAST",
                                nz(rule.message(), "%s must be in the future."
                                        .formatted(nz(field.label(), field.key()))), value.toString());
                    }
                }
                case EXACTLY_ONE_PRIMARY -> {
                    List<Map<String, Object>> rows = Maps.asMapList(level.get(field.key()));
                    long primaries = rows.stream()
                            .filter(r -> Boolean.TRUE.equals(r.get(nz(str(p.get("flag")), "isPrimary"))))
                            .count();
                    if (!rows.isEmpty() && primaries != 1 && ctx.mode == ValidationMode.SUBMIT) {
                        ctx.error(path, field.label(), "EXACTLY_ONE_PRIMARY_REQUIRED",
                                nz(rule.message(), "Exactly one entry must be marked primary."), primaries);
                    }
                }
                case ATTESTATION -> {
                    if (!Boolean.TRUE.equals(level.get(field.key())) && ctx.mode == ValidationMode.SUBMIT) {
                        ctx.error(path, field.label(), "ATTESTATION_REQUIRED",
                                nz(rule.message(), "You must confirm this declaration before submitting."),
                                level.get(field.key()));
                    }
                }
                case AT_LEAST_ONE_OF -> {
                    List<Object> keys = Maps.asList(p.get("fields"));
                    boolean any = keys != null && keys.stream().anyMatch(k -> !Maps.isBlank(level.get(str(k))));
                    if (!any && ctx.mode == ValidationMode.SUBMIT) {
                        ctx.error(path, field.label(), "AT_LEAST_ONE_OF",
                                nz(rule.message(), "At least one of these fields is required."), null);
                    }
                }
                case MUTUALLY_EXCLUSIVE -> {
                    Collection<Object> a = collectionOf(level.get(field.key()));
                    Collection<Object> b = collectionOf(level.get(str(p.get("against"))));
                    Set<Object> overlap = new HashSet<>(a);
                    overlap.retainAll(new HashSet<>(b));
                    if (!overlap.isEmpty()) {
                        ctx.error(path, field.label(), "KEYWORD_CONFLICT",
                                nz(rule.message(), "These values also appear in %s: %s."
                                        .formatted(str(p.get("against")), overlap)), overlap);
                    }
                }
                case CROSS_FIELD_COMPARE -> {
                    BigDecimal left = numberOf(level.get(field.key()));
                    BigDecimal right = numberOf(level.get(str(p.get("against"))));
                    if (left != null && right != null && left.compareTo(right) > 0) {
                        ctx.error(path, field.label(), "RANGE_INVERTED",
                                nz(rule.message(), "%s must not exceed %s."
                                        .formatted(nz(field.label(), field.key()), str(p.get("against")))),
                                left);
                    }
                }
                case MAX_LENGTH, RANGE, PATTERN, IN_VOCABULARY, ARRAY_CAP, ARRAY_MIN -> {
                    // Expressed declaratively on the FieldSchema itself and already applied above.
                }
            }
        }
    }

    private void checkTiers(FieldSchema field, Map<String, Object> level, String path, Ctx ctx,
                            Map<String, Object> params, RuleSpec rule) {
        List<Map<String, Object>> rows = Maps.asMapList(level.get(field.key()));
        String minKey = nz(str(params.get("min")), "minSamples");
        String maxKey = nz(str(params.get("max")), "maxSamples");
        BigDecimal previousMax = null;
        for (int i = 0; i < rows.size(); i++) {
            BigDecimal min = numberOf(rows.get(i).get(minKey));
            BigDecimal max = numberOf(rows.get(i).get(maxKey));
            if (min != null && previousMax != null && min.compareTo(previousMax) <= 0) {
                ctx.error(path + "[" + i + "]." + minKey, field.label(), "TIERS_NOT_CONTIGUOUS",
                        nz(rule.message(), "Tier %d starts at %s but tier %d ends at %s. Ranges must not overlap."
                                .formatted(i + 1, min, i, previousMax)), min);
                ctx.replaceLastMeta(Map.of("previousTierMax", previousMax));
            }
            if (min != null && max != null && max.compareTo(min) < 0) {
                ctx.error(path + "[" + i + "]." + maxKey, field.label(), "RANGE_INVERTED",
                        "Tier %d ends before it starts.".formatted(i + 1), max);
            }
            previousMax = max != null ? max : previousMax;
        }
    }

    // ------------------------------------------------------------------ helpers

    private void collectDeclared(List<SectionSchema> sections, Set<String> declared) {
        if (sections == null) {
            return;
        }
        for (SectionSchema section : sections) {
            String key = section.dataKey() != null ? section.dataKey() : section.key();
            if (key == null || key.isBlank()) {
                if (section.fields() != null) {
                    section.fields().forEach(f -> declared.add(f.key()));
                }
                collectDeclared(section.sections(), declared);
            } else {
                declared.add(key);
            }
        }
    }

    private void rejectUnknown(Map<String, Object> in, Set<String> declared, Ctx ctx) {
        if (!properties.getSave().isRejectUnknownDataKeys()) {
            return;
        }
        for (String key : in.keySet()) {
            if (!declared.contains(key)) {
                ctx.error(ctx.path(key), key, "UNKNOWN_FIELD",
                        "'%s' is not a field of this step.".formatted(key), null);
            }
        }
    }

    private Object normalizeForCompare(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private BigDecimal toNumber(Object raw) {
        if (raw instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        return new BigDecimal(String.valueOf(raw).trim());
    }

    private Boolean toBoolean(Object raw) {
        if (raw instanceof Boolean b) {
            return b;
        }
        String s = String.valueOf(raw).trim();
        if ("true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s)) {
            return Boolean.parseBoolean(s);
        }
        throw new IllegalArgumentException("not a boolean");
    }

    private BigDecimal numberOf(Object raw) {
        try {
            return raw == null ? null : toNumber(raw);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private LocalDate dateOf(Object raw) {
        DateNormalizer.Normalized n = dateNormalizer.normalize(raw);
        return n == null ? null : n.value();
    }

    private Collection<Object> collectionOf(Object raw) {
        List<Object> list = Maps.asList(raw);
        return list == null ? List.of() : list;
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String nz(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /** Accumulates errors, warnings and rejections while walking, and tracks the current data path. */
    private static final class Ctx {
        private final ValidationMode mode;
        private final String stepKey;
        private final List<String> segments = new ArrayList<>();
        private final List<FieldError> errors = new ArrayList<>();
        private final List<Warning> warnings = new ArrayList<>();
        private final List<RejectedField> rejected = new ArrayList<>();

        Ctx(ValidationMode mode, String stepKey, String rootPath) {
            this.mode = mode;
            this.stepKey = stepKey;
            this.segments.add(rootPath);
        }

        void push(String segment) {
            segments.add(segment);
        }

        void pop() {
            segments.remove(segments.size() - 1);
        }

        String path(String leaf) {
            return String.join(".", segments) + "." + leaf;
        }

        FieldError error(String path, String label, String code, String message, Object rejectedValue) {
            FieldError e = new FieldError(stepKey, path, label, code, message, rejectedValue, null, null);
            errors.add(e);
            return e;
        }

        void errorConditional(String path, String label, String code, String message, String condition) {
            errors.add(new FieldError(stepKey, path, label, code, message, null, condition, null));
        }

        void replaceLastMeta(Map<String, Object> meta) {
            if (errors.isEmpty()) {
                return;
            }
            int last = errors.size() - 1;
            errors.set(last, errors.get(last).withMeta(meta));
        }

        void warn(String path, String code, String message) {
            warnings.add(Warning.of(path, code, message));
        }

        void rejected(RejectedField field) {
            rejected.add(field);
        }

        ValidationResult result(Map<String, Object> sanitized) {
            return new ValidationResult(List.copyOf(errors), List.copyOf(warnings), List.copyOf(rejected),
                    sanitized);
        }
    }
}
