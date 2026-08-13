package com.beetloop.catalog.shared.model;

public record DocRef(String documentId, String fileName, String mimeType, Long sizeBytes) {
}
