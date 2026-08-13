package com.beetloop.catalog.document;

import java.io.IOException;
import java.io.InputStream;

/**
 * Port. The local implementation writes to disk so the service runs without cloud credentials;
 * swap in an S3 adapter (presigned PUT, short-TTL single-use GET) without touching the callers.
 */
public interface StorageGateway {

    String store(String vendorId, String documentId, InputStream content) throws IOException;

    String presignedUrl(String storageKey, String documentId);

    void delete(String storageKey) throws IOException;
}
