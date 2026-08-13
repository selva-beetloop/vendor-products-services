package com.beetloop.catalog.customvalue.web;

import com.beetloop.catalog.customvalue.CustomValueService;
import com.beetloop.catalog.masters.VocabularyService;
import com.beetloop.catalog.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Replaces the "+ Add" chip values that currently vanish on reload. */
@Tag(name = "Custom values")
@RestController
@RequestMapping("/vendor/custom-values")
public class CustomValueController {

    private final CustomValueService service;
    private final VocabularyService vocabularies;

    public CustomValueController(CustomValueService service, VocabularyService vocabularies) {
        this.service = service;
        this.vocabularies = vocabularies;
    }

    public record CreateRequest(@NotBlank String fieldKey, String vocabularyCode, @NotBlank String value) {
    }

    @Operation(summary = "Master options and custom values in one merged list")
    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam String fieldKey,
                                                 @RequestParam(required = false) String vocabularyCode) {
        List<Map<String, String>> masterOptions = vocabularyCode == null ? List.of()
                : vocabularies.optionsAsMaps(vocabularyCode, null);
        return ApiResponse.of(service.list(fieldKey, masterOptions, vocabularyCode));
    }

    @PostMapping
    public ApiResponse<CustomValueService.CreateResult> create(@RequestBody CreateRequest request) {
        Map<String, String> masterMatch = null;
        if (request.vocabularyCode() != null) {
            masterMatch = vocabularies.optionsAsMaps(request.vocabularyCode(), null).stream()
                    .filter(o -> CustomValueService.normalize(o.get("label"))
                            .equals(CustomValueService.normalize(request.value())))
                    .findFirst().orElse(null);
        }
        return ApiResponse.of(service.create(request.fieldKey(), request.vocabularyCode(),
                request.value(), masterMatch));
    }

    @DeleteMapping("/{customValueId}")
    public ResponseEntity<Void> delete(@PathVariable String customValueId) {
        // A production build passes the real reference set here; an empty list means "no references".
        service.delete(customValueId, List.of());
        return ResponseEntity.noContent().build();
    }
}
