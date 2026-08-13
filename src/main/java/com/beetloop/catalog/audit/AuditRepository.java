package com.beetloop.catalog.audit;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AuditRepository extends MongoRepository<AuditEvent, String> {
    List<AuditEvent> findByEntityIdOrderByAtDesc(String entityId);
}
