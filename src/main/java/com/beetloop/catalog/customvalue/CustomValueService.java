package com.beetloop.catalog.customvalue;

import com.beetloop.catalog.shared.error.ApiException;
import com.beetloop.catalog.shared.error.ErrorCode;
import com.beetloop.catalog.shared.tenant.TenantContext;
import com.beetloop.catalog.shared.util.Ids;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Rules the endpoint enforces:
 *  - deduped on (vendorId, fieldKey, normalizedValue)
 *  - a value colliding with a master option returns the MASTER code instead of creating a duplicate
 *  - master-supplied values are never editable or deletable here
 *  - deleting a value still referenced by a listing is a 409
 */
@Service
public class CustomValueService {

    private final CustomValueRepository repository;

    public CustomValueService(CustomValueRepository repository) {
        this.repository = repository;
    }

    public record MergedValue(String value, String source, String code, String customValueId,
                              boolean editable, boolean deletable, Integer usageCount) {
    }

    public record CreateResult(String customValueId, String value, String normalizedValue, String code,
                               String source, boolean created, int usageCount, String message) {
    }

    public static String normalize(String value) {
        return value == null ? null
                : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    /** Master options and custom values in ONE merged list, so the chip field renders without a second call. */
    public Map<String, Object> list(String fieldKey, List<Map<String, String>> masterOptions,
                                    String vocabularyCode) {
        List<MergedValue> values = new ArrayList<>();
        for (Map<String, String> option : masterOptions) {
            values.add(new MergedValue(option.get("label"), "MASTER", option.get("code"), null,
                    false, false, null));
        }
        for (CustomValue custom : repository.findByVendorIdAndFieldKey(TenantContext.vendorId(), fieldKey)) {
            values.add(new MergedValue(custom.getValue(), "CUSTOM", null, custom.getId(),
                    true, true, custom.getUsageCount()));
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fieldKey", fieldKey);
        payload.put("vocabularyCode", vocabularyCode);
        payload.put("values", values);
        return payload;
    }

    public CreateResult create(String fieldKey, String vocabularyCode, String rawValue,
                               Map<String, String> masterMatch) {
        String value = rawValue == null ? "" : rawValue.trim();
        if (value.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION, "value must not be blank.");
        }
        if (masterMatch != null) {
            return new CreateResult(null, masterMatch.get("label"), normalize(value),
                    masterMatch.get("code"), "MASTER", false, 0,
                    "This value already exists in the master vocabulary; the master code was used.");
        }
        String normalized = normalize(value);
        return repository.findByVendorIdAndFieldKeyAndNormalizedValue(
                        TenantContext.vendorId(), fieldKey, normalized)
                .map(existing -> new CreateResult(existing.getId(), existing.getValue(), normalized,
                        null, "CUSTOM", false, existing.getUsageCount(), null))
                .orElseGet(() -> {
                    CustomValue created = repository.save(CustomValue.builder()
                            .id(Ids.newId("cv"))
                            .vendorId(TenantContext.vendorId())
                            .fieldKey(fieldKey)
                            .vocabularyCode(vocabularyCode)
                            .value(value)
                            .normalizedValue(normalized)
                            .usageCount(0)
                            .createdAt(Instant.now())
                            .createdBy(TenantContext.userId())
                            .build());
                    return new CreateResult(created.getId(), created.getValue(), normalized, null,
                            "CUSTOM", true, 0, null);
                });
    }

    public void delete(String customValueId, List<Map<String, Object>> references) {
        CustomValue value = repository.findByIdAndVendorId(customValueId, TenantContext.vendorId())
                .orElseThrow(() -> ApiException.notFound("Custom value " + customValueId));
        if (references != null && !references.isEmpty()) {
            throw new ApiException(ErrorCode.CUSTOM_VALUE_IN_USE,
                    "'%s' is referenced by %d listing(s).".formatted(value.getValue(), references.size()))
                    .with("referencedBy", references);
        }
        repository.delete(value);
    }

    public boolean exists(String fieldKey, String rawValue) {
        TenantContext.Principal principal = TenantContext.currentOrNull();
        if (principal == null || principal.vendorId() == null) {
            return false;
        }
        return repository.findByVendorIdAndFieldKeyAndNormalizedValue(
                principal.vendorId(), fieldKey, normalize(rawValue)).isPresent();
    }
}
