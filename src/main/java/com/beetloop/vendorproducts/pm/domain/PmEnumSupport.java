package com.beetloop.vendorproducts.pm.domain;

import java.util.Locale;

/**
 * Shared lenient parsing for the PM enums.
 *
 * <p>Accepts the enum name, its human label, and kebab/space variants, so a
 * client may send {@code IN_STOCK}, {@code "In stock"} or {@code in-stock}
 * interchangeably. Same tolerance the products module applies to its category
 * and status parameters.
 */
final class PmEnumSupport {

    private PmEnumSupport() {
    }

    static <E extends Enum<E>> E match(E[] values, String raw, String what) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String needle = normalise(raw);
        for (E value : values) {
            if (normalise(value.name()).equals(needle)) {
                return value;
            }
            String label = labelOf(value);
            if (label != null && normalise(label).equals(needle)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown " + what + " '" + raw + "'");
    }

    private static String labelOf(Enum<?> value) {
        try {
            return (String) value.getClass().getMethod("getLabel").invoke(value);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static String normalise(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
