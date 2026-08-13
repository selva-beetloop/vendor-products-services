package com.beetloop.catalog.shared.api;

import org.springframework.data.domain.Page;

public record PageMeta(int page, int size, long totalElements, int totalPages) {

    public static PageMeta of(Page<?> page) {
        return new PageMeta(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    public static PageMeta of(int page, int size, long totalElements) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageMeta(page, size, totalElements, totalPages);
    }
}
