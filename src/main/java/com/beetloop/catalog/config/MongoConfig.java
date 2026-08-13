package com.beetloop.catalog.config;

import com.beetloop.catalog.shared.tenant.TenantContext;
import com.mongodb.client.MongoClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Optional;

@Configuration
public class MongoConfig {

    /**
     * The overall save, submit-qc and every QC transition are multi-document. This is why the
     * deployment target is a replica set and not a standalone mongod.
     */
    @Bean
    PlatformTransactionManager mongoTransactionManager(MongoDatabaseFactory factory) {
        return new MongoTransactionManager(factory);
    }

    @Bean
    AuditorAware<String> auditorAware() {
        return () -> Optional.ofNullable(TenantContext.currentOrNull())
                .map(TenantContext.Principal::userId);
    }

    @Bean
    MongoClientHealthMarker mongoClientHealthMarker(MongoClient client) {
        return new MongoClientHealthMarker(client);
    }

    /** Trivial holder so the client is eagerly created at startup rather than on first request. */
    public record MongoClientHealthMarker(MongoClient client) {
    }
}
