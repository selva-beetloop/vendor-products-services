package com.beetloop.vendorproducts;

import de.flapdoodle.embed.mongo.commands.MongodArguments;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@TestConfiguration
@Profile("test")
public class EmbeddedMongoConfig {

    @Bean
    MongodArguments mongodArguments() {
        return MongodArguments.builder()
                .useNoJournal(false)
                .useNoPrealloc(false)
                .useSmallFiles(false)
                .build();
    }
}
