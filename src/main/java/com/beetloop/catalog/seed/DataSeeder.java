package com.beetloop.catalog.seed;

import com.beetloop.catalog.config.CatalogProperties;
import com.beetloop.catalog.masters.MasterCatalogEntry;
import com.beetloop.catalog.masters.MasterCatalogRepository;
import com.beetloop.catalog.masters.MasterCategory;
import com.beetloop.catalog.masters.MasterCategoryRepository;
import com.beetloop.catalog.masters.Vocabulary;
import com.beetloop.catalog.masters.VocabularyRepository;
import com.beetloop.catalog.template.FormTemplateRepository;
import com.beetloop.catalog.template.model.FormTemplate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * Seeds the form templates, vocabularies, categories and commercial-master rows.
 *
 * The plan calls for Mongock; this build uses an idempotent startup seeder plus
 * spring.data.mongodb.auto-index-creation so the service runs with one `docker compose up`.
 * Swapping in Mongock changes only this class.
 */
@Slf4j
@Component
public class DataSeeder implements ApplicationRunner {

    private final CatalogProperties properties;
    private final ObjectMapper objectMapper;
    private final FormTemplateRepository templates;
    private final VocabularyRepository vocabularies;
    private final MasterCategoryRepository categories;
    private final MasterCatalogRepository masterCatalog;

    public DataSeeder(CatalogProperties properties, ObjectMapper objectMapper,
                      FormTemplateRepository templates, VocabularyRepository vocabularies,
                      MasterCategoryRepository categories, MasterCatalogRepository masterCatalog) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.templates = templates;
        this.vocabularies = vocabularies;
        this.categories = categories;
        this.masterCatalog = masterCatalog;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.getSeed().isEnabled()) {
            return;
        }
        seedCategories();
        seedVocabularies();
        seedMasterCatalog();
        seedTemplates();
    }

    private void seedCategories() {
        if (categories.count() > 0) {
            return;
        }
        List<MasterCategory> rows = read("seed/categories.json", new TypeReference<>() {
        });
        categories.saveAll(rows);
        log.info("Seeded {} master categories", rows.size());
    }

    private void seedVocabularies() {
        if (vocabularies.count() > 0) {
            return;
        }
        List<Vocabulary> rows = read("seed/vocabularies.json", new TypeReference<>() {
        });
        vocabularies.saveAll(rows);
        log.info("Seeded {} vocabularies", rows.size());
    }

    private void seedMasterCatalog() {
        if (masterCatalog.count() > 0) {
            return;
        }
        List<MasterCatalogEntry> rows = read("seed/master-catalog.json", new TypeReference<>() {
        });
        masterCatalog.saveAll(rows);
        log.info("Seeded {} commercial master entries", rows.size());
    }

    /** One JSON file per category. Adding a category should be a template file and nothing else. */
    private void seedTemplates() {
        if (templates.count() > 0) {
            return;
        }
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:seed/templates/*.json");
            for (Resource resource : resources) {
                try (InputStream in = resource.getInputStream()) {
                    FormTemplate template = objectMapper.readValue(in, FormTemplate.class);
                    templates.save(template);
                    log.info("Seeded form template {} v{} ({} steps, {} child sections)",
                            template.categoryCode(), template.version(),
                            template.steps() == null ? 0 : template.steps().size(),
                            template.childSections() == null ? 0 : template.childSections().size());
                }
            }
        } catch (Exception e) {
            log.error("Form template seeding failed", e);
        }
    }

    private <T> List<T> read(String path, TypeReference<List<T>> type) {
        try (InputStream in = new PathMatchingResourcePatternResolver()
                .getResource("classpath:" + path).getInputStream()) {
            return objectMapper.readValue(in, type);
        } catch (Exception e) {
            log.error("Could not read seed file {}", path, e);
            return List.of();
        }
    }
}
