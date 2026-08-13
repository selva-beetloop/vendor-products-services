package com.beetloop.catalog.shared.util;

import java.util.Locale;

/**
 * Step and section keys travel as kebab-case in URLs (`technical-specifications`) and are stored as
 * camelCase in data{} (`technicalSpecifications`).
 */
public final class Keys {

    private Keys() {
    }

    public static String toCamel(String kebab) {
        if (kebab == null || kebab.isBlank()) {
            return kebab;
        }
        if (kebab.indexOf('-') < 0) {
            return kebab;
        }
        String[] parts = kebab.toLowerCase(Locale.ROOT).split("-");
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
        }
        return sb.toString();
    }

    public static String toKebab(String camel) {
        if (camel == null || camel.isBlank()) {
            return camel;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : camel.toCharArray()) {
            if (Character.isUpperCase(c)) {
                sb.append('-').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
