package com.beetloop.catalog.masters.web;

import com.beetloop.catalog.masters.CountryService;
import com.beetloop.catalog.masters.MasterCatalogEntry;
import com.beetloop.catalog.masters.MasterCategory;
import com.beetloop.catalog.masters.MasterCategoryRepository;
import com.beetloop.catalog.masters.MasterSearchService;
import com.beetloop.catalog.masters.Vocabulary;
import com.beetloop.catalog.masters.VocabularyService;
import com.beetloop.catalog.shared.api.ApiResponse;
import com.beetloop.catalog.shared.api.PagedResponse;
import com.beetloop.catalog.shared.error.ApiException;
import com.beetloop.catalog.shared.model.ListingType;
import com.beetloop.catalog.template.TemplateService;
import com.beetloop.catalog.template.model.FormTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Masters", description = "Categories, form templates, vocabularies, master catalogue search")
@RestController
@RequestMapping("/masters")
public class MastersController {

    private final MasterCategoryRepository categories;
    private final TemplateService templates;
    private final VocabularyService vocabularies;
    private final CountryService countries;
    private final MasterSearchService search;

    public MastersController(MasterCategoryRepository categories, TemplateService templates,
                             VocabularyService vocabularies, CountryService countries,
                             MasterSearchService search) {
        this.categories = categories;
        this.templates = templates;
        this.vocabularies = vocabularies;
        this.countries = countries;
        this.search = search;
    }

    @GetMapping("/product-categories")
    public ApiResponse<List<MasterCategory>> productCategories() {
        return ApiResponse.of(categories.findByTypeOrderByOrderAsc(ListingType.PRODUCT));
    }

    @GetMapping("/service-categories")
    public ApiResponse<List<MasterCategory>> serviceCategories() {
        return ApiResponse.of(categories.findByTypeOrderByOrderAsc(ListingType.SERVICE));
    }

    @Operation(summary = "The form template that drives rendering AND validation for a category. "
            + "Pass ?version= to re-render an in-flight draft exactly as it was authored.")
    @GetMapping("/form-templates/{categoryCode}")
    public ApiResponse<FormTemplate> formTemplate(@PathVariable String categoryCode,
                                                  @RequestParam(required = false) Integer version) {
        return ApiResponse.of(version == null
                ? templates.active(categoryCode)
                : templates.version(categoryCode, version));
    }

    @Operation(summary = "Option list. ?parent= scopes a cascading child to its selected parent.")
    @GetMapping("/vocabularies/{vocabularyCode}")
    public ApiResponse<Map<String, Object>> vocabulary(@PathVariable String vocabularyCode,
                                                       @RequestParam(required = false) String parent) {
        Vocabulary vocabulary = vocabularies.require(vocabularyCode);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", vocabulary.getCode());
        payload.put("label", vocabulary.getLabel());
        payload.put("parentVocabularyCode", vocabulary.getParentVocabularyCode());
        payload.put("parent", parent);
        payload.put("options", vocabularies.options(vocabularyCode, parent));
        return ApiResponse.of(payload);
    }

    @GetMapping("/countries")
    public ApiResponse<List<Map<String, String>>> countries(@RequestParam(required = false) String q) {
        return ApiResponse.of(countries.search(q));
    }

    @PostMapping("/products/search")
    public PagedResponse<MasterCatalogEntry> searchProducts(
            @RequestBody MasterSearchService.SearchRequest request) {
        requireCategory(request);
        return search.search(ListingType.PRODUCT, request);
    }

    @PostMapping("/services/search")
    public PagedResponse<MasterCatalogEntry> searchServices(
            @RequestBody MasterSearchService.SearchRequest request) {
        requireCategory(request);
        return search.search(ListingType.SERVICE, request);
    }

    private void requireCategory(MasterSearchService.SearchRequest request) {
        if (request.categoryCode() == null || request.categoryCode().isBlank()) {
            throw new ApiException(com.beetloop.catalog.shared.error.ErrorCode.VALIDATION,
                    "categoryCode is required: the search facets differ per category.");
        }
    }
}
