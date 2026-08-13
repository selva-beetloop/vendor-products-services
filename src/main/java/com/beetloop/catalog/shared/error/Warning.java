package com.beetloop.catalog.shared.error;

import java.util.Map;

/** Never blocks a save. Returned so the wizard can nudge without preventing a draft write. */
public record Warning(String path, String code, String message, Map<String, Object> meta) {

    public static Warning of(String path, String code, String message) {
        return new Warning(path, code, message, null);
    }

    public static Warning of(String path, String code, String message, Map<String, Object> meta) {
        return new Warning(path, code, message, meta);
    }
}
