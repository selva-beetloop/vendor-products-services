package com.beetloop.vendorproducts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.LinkedHashMap;
import java.util.Map;

/** {@code PUT /products/{id}/role} — Step 2 "Your Role &amp; Supply Information". */
@Schema(description = "Step 2 (Your Role) payload.")
public record RoleStepRequest(

        @NotBlank(message = "Role is required")
        @Schema(description = "Id of the selected role card.", example = "manufacturer")
        String roleId,

        @Schema(description = "Flat map of role field name → value for the selected role card.")
        Map<String, Object> data,

        @Schema(defaultValue = "false")
        Boolean draft) {

    public Map<String, Object> dataOrEmpty() {
        return data == null ? new LinkedHashMap<>() : data;
    }

    public boolean isDraft() {
        return Boolean.TRUE.equals(draft);
    }
}
