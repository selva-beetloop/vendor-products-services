package com.beetloop.catalog.masters;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface VocabularyRepository extends MongoRepository<Vocabulary, String> {
    Optional<Vocabulary> findByCode(String code);
}
