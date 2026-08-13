package com.beetloop.catalog.qc;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface StatusHistoryRepository extends MongoRepository<StatusHistory, String> {
    List<StatusHistory> findByEntityIdOrderByAtAsc(String entityId);
}
