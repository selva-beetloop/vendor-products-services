package com.beetloop.catalog.shared.error;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Carries the per-step, per-field error map. The frontend has no error state of its own and the five
 * product categories disagree about whether to block at all, so one payload has to render three
 * ways: as a banner, as per-field messages, and as per-panel badges on the review page.
 */
@Getter
public class ValidationException extends ApiException {

    /** The exact string the Processing Machinery wizard already shows. */
    public static final String DEFAULT_BANNER =
            "Please fill in all required fields marked with * before continuing.";

    private final transient List<FieldError> fieldErrors;
    private final transient List<Warning> warnings;
    private final String bannerMessage;

    public ValidationException(List<FieldError> fieldErrors, List<Warning> warnings) {
        super(ErrorCode.VALIDATION, buildDetail(fieldErrors));
        this.fieldErrors = List.copyOf(fieldErrors);
        this.warnings = warnings == null ? List.of() : List.copyOf(warnings);
        this.bannerMessage = DEFAULT_BANNER;
    }

    private static String buildDetail(List<FieldError> errors) {
        long steps = errors.stream().map(FieldError::step).filter(java.util.Objects::nonNull).distinct().count();
        return "%d field%s failed validation across %d step%s.".formatted(
                errors.size(), errors.size() == 1 ? "" : "s", steps, steps == 1 ? "" : "s");
    }

    /** step -> error count, for the stepper badges and the review panels. */
    public Map<String, Integer> stepErrors() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (FieldError e : fieldErrors) {
            String step = e.step() == null ? "_" : e.step();
            counts.merge(step, 1, Integer::sum);
        }
        return counts;
    }
}
