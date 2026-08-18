package com.beetloop.vendorproducts.services.repository;

import com.beetloop.vendorproducts.services.domain.ServiceCategory;
import com.beetloop.vendorproducts.services.domain.ServiceStatus;
import com.beetloop.vendorproducts.services.domain.VendorService;
import com.beetloop.vendorproducts.services.domain.VendorServiceBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Repository
public class VendorServiceRepositoryImpl implements VendorServiceRepository {

    private final MongoTemplate mongoTemplate;

    public VendorServiceRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Page<VendorService> search(String vendorId,
                                      ServiceCategory category,
                                      ServiceStatus status,
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
        Query query = parts.isEmpty() ? new Query() : new Query(new Criteria().andOperator(parts));
        List<VendorServiceBatch> batches = mongoTemplate.find(query, VendorServiceBatch.class);

        String needle = search == null ? "" : search.toLowerCase(Locale.ROOT);
        List<VendorService> items = new ArrayList<>();
        for (VendorServiceBatch batch : batches) {
            if (batch.getItems() == null) {
                continue;
            }
            for (VendorService item : batch.getItems()) {
                item.setBatch(batch);
                if (matches(item, needle)) {
                    items.add(item);
                }
            }
        }

        int from = Math.min((int) pageable.getOffset(), items.size());
        int to = Math.min(from + pageable.getPageSize(), items.size());
        return new PageImpl<>(items.subList(from, to), pageable, items.size());
    }

    @Override
    public Optional<VendorService> findItem(UUID itemId, ServiceStatus requiredStatus) {
        List<Criteria> parts = new ArrayList<>();
        parts.add(Criteria.where("items.id").is(itemId));
        if (requiredStatus != null) {
            parts.add(Criteria.where("status").is(requiredStatus));
        }
        Query query = new Query(new Criteria().andOperator(parts));
        VendorServiceBatch batch = mongoTemplate.findOne(query, VendorServiceBatch.class);
        if (batch == null || batch.getItems() == null) {
            return Optional.empty();
        }
        for (VendorService item : batch.getItems()) {
            if (itemId.equals(item.getId())) {
                item.setBatch(batch);
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    private static boolean matches(VendorService item, String needle) {
        if (needle.isEmpty()) {
            return true;
        }
        return contains(item.getName(), needle)
                || contains(item.getSku(), needle)
                || contains(item.getCategoryLabel(), needle);
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }
}
