package com.beetloop.catalog.shared.idempotency;

import com.beetloop.catalog.shared.error.ApiException;
import com.beetloop.catalog.shared.error.ErrorCode;
import com.beetloop.catalog.shared.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Honoured on save-all, submit-qc, document upload and bulk actions.
 * Same key + same body -> the original response is replayed.
 * Same key + different body -> 422 BL-PS-422-IDEMPOTENCY-MISMATCH.
 */
@Slf4j
@Service
public class IdempotencyService {

    private final IdempotencyRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public <T> T execute(String idempotencyKey, String endpoint, Object request, Class<T> responseType,
                         Supplier<T> action) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return action.get();
        }
        String digest = digest(request);
        String storageId = TenantContext.vendorId() + ":" + idempotencyKey;

        Optional<IdempotencyRecord> existing = repository.findById(storageId);
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (!record.getRequestDigest().equals(digest)) {
                throw new ApiException(ErrorCode.IDEMPOTENCY_MISMATCH,
                        "Idempotency-Key %s was first used with a different request body."
                                .formatted(idempotencyKey))
                        .with("originalRequestAt", record.getCreatedAt());
            }
            return read(record.getResponseJson(), responseType);
        }

        T response = action.get();
        try {
            repository.save(new IdempotencyRecord(storageId, TenantContext.vendorId(), endpoint, digest,
                    objectMapper.writeValueAsString(response), 200, Instant.now()));
        } catch (Exception e) {
            // Never fail a successful write because the replay record could not be stored.
            log.warn("Could not persist idempotency record {}: {}", storageId, e.getMessage());
        }
        return response;
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.INTERNAL, "Stored idempotent response could not be replayed.");
        }
    }

    private String digest(Object request) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(request);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception e) {
            return HexFormat.of().formatHex(String.valueOf(request).getBytes(StandardCharsets.UTF_8));
        }
    }
}
