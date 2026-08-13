package com.beetloop.catalog.qc;

import com.beetloop.catalog.shared.model.QcStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface QcReviewRepository extends MongoRepository<QcReview, String> {

    Page<QcReview> findByStatus(QcStatus status, Pageable pageable);

    Page<QcReview> findByStatusAndCategoryCode(QcStatus status, String categoryCode, Pageable pageable);

    Page<QcReview> findByStatusAndEntityType(QcStatus status, String entityType, Pageable pageable);

    List<QcReview> findByEntityIdOrderByRevisionDesc(String entityId);

    Optional<QcReview> findFirstByEntityIdOrderByRevisionDesc(String entityId);

    long countByStatus(QcStatus status);
}
