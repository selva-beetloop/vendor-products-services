package com.beetloop.catalog.shared.util;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

/** Atomic counters behind the human-readable codes: PRD-2026-000131, SVC-2026-000087, REQ-RM-2026-0042. */
@Service
public class SequenceService {

    private static final String COLLECTION = "sequences";

    private final MongoOperations mongo;

    public SequenceService(MongoOperations mongo) {
        this.mongo = mongo;
    }

    public long next(String sequenceName) {
        Map<?, ?> result = mongo.findAndModify(
                Query.query(Criteria.where("_id").is(sequenceName)),
                new Update().inc("value", 1L),
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                Map.class,
                COLLECTION);
        Object value = result == null ? null : result.get("value");
        return value instanceof Number n ? n.longValue() : 1L;
    }

    private int currentYear() {
        return LocalDate.now(ZoneOffset.UTC).getYear();
    }

    /** PRD-2026-000131 */
    public String listingCode(String prefix) {
        int year = currentYear();
        long n = next("%s-%d".formatted(prefix, year));
        return "%s-%d-%06d".formatted(prefix, year, n);
    }

    /** REQ-RM-2026-0042 — assigned on the "Add New Material" escape hatch (Path B). */
    public String requestCode(String categoryAbbrev) {
        int year = currentYear();
        long n = next("REQ-%s-%d".formatted(categoryAbbrev, year));
        return "REQ-%s-%d-%04d".formatted(categoryAbbrev, year, n);
    }
}
