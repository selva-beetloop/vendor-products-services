package com.beetloop.catalog.shared.api;

import java.util.List;

/** Collections carry a sibling `page`, and optionally a `meta` block (facets, counters, grid columns). */
public record PagedResponse<T>(List<T> data, PageMeta page, Object meta) {

    public static <T> PagedResponse<T> of(List<T> data, PageMeta page) {
        return new PagedResponse<>(data, page, null);
    }

    public static <T> PagedResponse<T> of(List<T> data, PageMeta page, Object meta) {
        return new PagedResponse<>(data, page, meta);
    }
}
