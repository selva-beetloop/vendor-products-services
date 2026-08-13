package com.beetloop.catalog.shared.model;

/**
 * MASTER      - "Add to Catalog" on an existing commercial-master row; master fields are LINKED.
 * REQUEST_NEW - the "Add New Material" escape hatch; every field editable, a Request Code assigned.
 */
public enum EntryPath {
    MASTER,
    REQUEST_NEW
}
