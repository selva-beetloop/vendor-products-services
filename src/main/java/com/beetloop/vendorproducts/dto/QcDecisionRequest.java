package com.beetloop.vendorproducts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** {@code PUT /products/{id}/qc-decision} — the QC reviewer's verdict. */
@Schema(description = "QC decision on a submitted product.")
public record QcDecisionRequest(

        @NotBlank(message = "Decision is required")
        @Pattern(regexp = "(?i)APPROVE|REJECT|QUERY|PUBLISH",
                message = "Decision must be one of APPROVE, REJECT, QUERY, PUBLISH")
        @Schema(example = "APPROVE", allowableValues = {"APPROVE", "REJECT", "QUERY", "PUBLISH"})
        String decision,

        @Schema(description = "Reviewer name / id shown on the QC list.")
        String reviewer,

        @Schema(description = "Reviewer notes. Required when rejecting or raising a query.")
        String remarks) {
}
