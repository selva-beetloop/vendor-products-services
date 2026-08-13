package com.beetloop.catalog.template;

import com.beetloop.catalog.template.model.FormTemplate;
import com.beetloop.catalog.template.model.TemplateStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface FormTemplateRepository extends MongoRepository<FormTemplate, String> {

    Optional<FormTemplate> findFirstByCategoryCodeAndStatusOrderByVersionDesc(String categoryCode,
                                                                             TemplateStatus status);

    Optional<FormTemplate> findByCategoryCodeAndVersion(String categoryCode, int version);

    List<FormTemplate> findByStatus(TemplateStatus status);
}
