package com.beetloop.catalog.shared.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Null-safe helpers over the untyped data{} sub-documents. */
public final class Maps {

    private Maps() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> asList(Object value) {
        return value instanceof List<?> l ? (List<Object>) l : null;
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> asMapList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                out.add((Map<String, Object>) m);
            }
        }
        return out;
    }

    public static Map<String, Object> mapAt(Map<String, Object> parent, String key) {
        if (parent == null) {
            return null;
        }
        return asMap(parent.get(key));
    }

    public static Map<String, Object> orEmpty(Map<String, Object> value) {
        return value == null ? new LinkedHashMap<>() : value;
    }

    public static String str(Map<String, Object> parent, String key) {
        if (parent == null) {
            return null;
        }
        Object v = parent.get(key);
        return v == null ? null : String.valueOf(v);
    }

    public static boolean isBlank(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof CharSequence cs) {
            return cs.toString().isBlank();
        }
        if (value instanceof java.util.Collection<?> c) {
            return c.isEmpty();
        }
        if (value instanceof Map<?, ?> m) {
            return m.isEmpty();
        }
        return false;
    }

    /** Deep copy so a stored sub-document is never aliased into a response we then mutate. */
    public static Map<String, Object> deepCopy(Map<String, Object> source) {
        if (source == null) {
            return null;
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        source.forEach((k, v) -> copy.put(k, deepCopyValue(v)));
        return copy;
    }

    private static Object deepCopyValue(Object value) {
        if (value instanceof Map<?, ?> m) {
            Map<String, Object> copy = new LinkedHashMap<>();
            m.forEach((k, v) -> copy.put(String.valueOf(k), deepCopyValue(v)));
            return copy;
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            for (Object item : list) {
                copy.add(deepCopyValue(item));
            }
            return copy;
        }
        return value;
    }
}
