package com.beetloop.vendorproducts.repository;

import com.beetloop.vendorproducts.domain.ProductCategory;
import com.beetloop.vendorproducts.domain.ProductStatus;
import com.beetloop.vendorproducts.domain.VendorProduct;
import com.beetloop.vendorproducts.persistence.MongoQueries;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class VendorProductRepositoryImpl implements VendorProductRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public VendorProductRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Page<VendorProduct> search(String vendorId,
                                      ProductCategory category,
                                      ProductStatus status,
                                      Collection<ProductStatus> statuses,
                                      String search,
                                      Pageable pageable) {
        List<Criteria> parts = new ArrayList<>();
        if (vendorId != null) {
            parts.add(Criteria.where("vendorId").is(vendorId));
        }
        if (category != null) {
            parts.add(Criteria.where("category").is(category));
        }
        if (status != null) {
            parts.add(Criteria.where("status").is(status));
        }
        if (statuses != null && !statuses.isEmpty()) {
            parts.add(Criteria.where("status").in(statuses));
        }
        if (search != null && !search.isEmpty()) {
            parts.add(new Criteria().orOperator(
                    MongoQueries.containsIgnoreCase("name", search),
                    MongoQueries.containsIgnoreCase("sku", search),
                    MongoQueries.containsIgnoreCase("listingCategory", search)));
        }
        Query query = parts.isEmpty() ? new Query() : new Query(new Criteria().andOperator(parts));
        return MongoQueries.findPage(mongoTemplate, query, VendorProduct.class, pageable);
    }
}
