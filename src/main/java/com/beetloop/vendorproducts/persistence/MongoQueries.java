package com.beetloop.vendorproducts.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.regex.Pattern;

public final class MongoQueries {

    private MongoQueries() {
    }

    public static Criteria containsIgnoreCase(String field, String search) {
        return Criteria.where(field).regex(".*" + Pattern.quote(search) + ".*", "i");
    }

    public static <T> Page<T> findPage(MongoTemplate mongo, Query query, Class<T> type, Pageable pageable) {
        long total = mongo.count(query, type);
        query.with(pageable);
        List<T> content = mongo.find(query, type);
        return new PageImpl<>(content, pageable, total);
    }
}
