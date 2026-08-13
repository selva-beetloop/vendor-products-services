package com.beetloop.catalog.shared.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The certificate modal's `Date` and `Expiry Date` are free text, not date pickers, so the vendor
 * types whatever they like. We parse, normalise to ISO-8601, and echo which pattern matched so a
 * vendor can catch a wrong reading of an ambiguous value like 01/02/2026.
 */
@Component
public class DateNormalizer {

    public record Normalized(LocalDate value, String pattern) {
    }

    private record Candidate(String pattern, DateTimeFormatter formatter) {
    }

    /** Order matters: dd/MM before MM/dd, because the app's locale is India. */
    private static final List<Candidate> CANDIDATES = List.of(
            candidate("yyyy-MM-dd"),
            candidate("dd MMM yyyy"),
            candidate("d MMM yyyy"),
            candidate("dd MMMM yyyy"),
            candidate("MMM dd, yyyy"),
            candidate("MMM d, yyyy"),
            candidate("dd/MM/yyyy"),
            candidate("d/M/yyyy"),
            candidate("dd-MM-yyyy"),
            candidate("dd.MM.yyyy"));

    private static Candidate candidate(String pattern) {
        return new Candidate(pattern,
                DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH));
    }

    public static final List<String> ACCEPTED_FORMATS = CANDIDATES.stream().map(Candidate::pattern).toList();

    /** @return null when the input is blank; never throws for blank input. */
    public Normalized normalize(Object raw) {
        if (raw == null) {
            return null;
        }
        String text = String.valueOf(raw).trim();
        if (text.isEmpty()) {
            return null;
        }
        if (raw instanceof LocalDate d) {
            return new Normalized(d, "yyyy-MM-dd");
        }
        for (Candidate c : CANDIDATES) {
            try {
                return new Normalized(LocalDate.parse(text, c.formatter()), c.pattern());
            } catch (DateTimeParseException ignored) {
                // try the next pattern
            }
        }
        return null;
    }

    public Map<String, Object> acceptedFormatsPayload() {
        return Map.of("acceptedFormats", ACCEPTED_FORMATS);
    }
}
