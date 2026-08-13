package com.beetloop.catalog.masters;

import com.beetloop.catalog.customvalue.CustomValueService;
import com.beetloop.catalog.shared.error.ApiException;
import com.beetloop.catalog.template.VocabularyPort;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class VocabularyService implements VocabularyPort {

    private final VocabularyRepository repository;
    private final CustomValueService customValues;

    public VocabularyService(VocabularyRepository repository, CustomValueService customValues) {
        this.repository = repository;
        this.customValues = customValues;
    }

    @Cacheable(cacheNames = "vocabulary", key = "#code")
    public Optional<Vocabulary> find(String code) {
        return repository.findByCode(code);
    }

    public Vocabulary require(String code) {
        return find(code).orElseThrow(() -> ApiException.notFound("Vocabulary " + code));
    }

    /**
     * GET /masters/vocabularies/SECTOR?parent=FOOD returns only the children of FOOD - which is what
     * every cascading select needs, and why they render empty until their parent resolves.
     */
    public List<Vocabulary.Option> options(String code, String parent) {
        Vocabulary vocabulary = require(code);
        List<Vocabulary.Option> options = vocabulary.getOptions() == null
                ? List.of() : vocabulary.getOptions();
        return options.stream()
                .filter(Vocabulary.Option::isActive)
                .filter(o -> parent == null || parent.equals(o.getParentCode()))
                .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
                .toList();
    }

    public List<Map<String, String>> optionsAsMaps(String code, String parent) {
        return options(code, parent).stream()
                .map(o -> Map.of("code", o.getCode(), "label", o.getLabel()))
                .toList();
    }

    // ------------------------------------------------------------------ VocabularyPort

    @Override
    public boolean contains(String vocabularyCode, String parentCode, String value) {
        if (vocabularyCode == null || value == null) {
            return true;
        }
        Optional<Vocabulary> vocabulary = find(vocabularyCode);
        if (vocabulary.isEmpty() || vocabulary.get().getOptions() == null
                || vocabulary.get().getOptions().isEmpty()) {
            // An unseeded vocabulary must not block a save; the submit gate is where completeness bites.
            return true;
        }
        return vocabulary.get().getOptions().stream()
                .filter(o -> parentCode == null || parentCode.equals(o.getParentCode()))
                .anyMatch(o -> o.getCode().equals(value) || value.equals(o.getLabel()));
    }

    @Override
    public List<String> codes(String vocabularyCode, String parentCode) {
        return options(vocabularyCode, parentCode).stream().map(Vocabulary.Option::getCode).toList();
    }

    @Override
    public boolean isCustomValue(String fieldKey, String value) {
        return customValues.exists(fieldKey, value);
    }
}
