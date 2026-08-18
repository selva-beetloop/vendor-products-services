package com.beetloop.vendorproducts.catalogue.repository;

import com.beetloop.vendorproducts.catalogue.domain.CatalogueKind;
import com.beetloop.vendorproducts.catalogue.domain.CatalogueStatus;
import com.beetloop.vendorproducts.catalogue.domain.ScientificMaster;
import com.beetloop.vendorproducts.persistence.MongoQueries;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ScientificMasterRepositoryImpl implements ScientificMasterRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public ScientificMasterRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Page<ScientificMaster> search(CatalogueKind kind,
                                         String category,
                                         Collection<CatalogueStatus> statuses,
                                         String search,
                                         Pageable pageable) {
        List<Criteria> parts = new ArrayList<>();
        if (kind != null) {
            parts.add(Criteria.where("kind").is(kind));
        }
        if (category != null) {
            parts.add(Criteria.where("category").is(category));
        }
        if (statuses != null && !statuses.isEmpty()) {
            parts.add(Criteria.where("status").in(statuses));
        }
        if (search != null && !search.isEmpty()) {
            parts.add(new Criteria().orOperator(
                    MongoQueries.containsIgnoreCase("name", search),
                    MongoQueries.containsIgnoreCase("casNumber", search),
                    MongoQueries.containsIgnoreCase("code", search)));
        }
        Query query = parts.isEmpty() ? new Query() : new Query(new Criteria().andOperator(parts));
        return MongoQueries.findPage(mongoTemplate, query, ScientificMaster.class, pageable);
    }
}
