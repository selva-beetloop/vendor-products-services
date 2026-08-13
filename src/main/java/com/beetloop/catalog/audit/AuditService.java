package com.beetloop.catalog.audit;

import com.beetloop.catalog.shared.tenant.TenantContext;
import com.beetloop.catalog.shared.util.Ids;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
public class AuditService {

    private final AuditRepository repository;

    public AuditService(AuditRepository repository) {
        this.repository = repository;
    }

    public void record(String action, String entityType, String entityId, Map<String, Object> detail) {
        try {
            repository.save(AuditEvent.builder()
                    .id(Ids.newId("aud"))
                    .vendorId(TenantContext.currentOrNull() == null ? null : TenantContext.current().vendorId())
                    .actorId(TenantContext.currentOrNull() == null ? null : TenantContext.current().userId())
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .detail(detail)
                    .requestId(TenantContext.requestId())
                    .at(Instant.now())
                    .build());
        } catch (RuntimeException e) {
            // Auditing must never break the write it is recording.
            log.warn("Audit write failed for {} {}: {}", action, entityId, e.getMessage());
        }
    }
}
