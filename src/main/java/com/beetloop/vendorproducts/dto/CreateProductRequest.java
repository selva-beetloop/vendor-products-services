package com.beetloop.vendorproducts.dto;

import com.beetloop.vendorproducts.domain.ProductCategory;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * {@code POST /products} and {@code POST /listings} — Flow A sends a live T2 FK;
 * Flow B sends a live T1 plus grade-defining fields.
 */
@Schema(description = "Creates a T3 vendor listing. Prefer commercialMasterId (T2 code or UUID).")
public record CreateProductRequest(

        @Schema(example = "raw-materials",
                allowableValues = {"raw-materials", "processing-machinery", "finished-goods",
                        "packaging-materials", "packaging-machinery"})
        ProductCategory category,

        @Schema(description = "Type card chosen inside Step 1, when the category has a type selector.",
                example = "botanical-extract")
        String identityType,

        @Schema(description = "Deprecated alias of the T2 code.",
                example = "CM-CUR95-001")
        String sourceMasterId,

        @Schema(description = "Working name shown on the catalog card until Step 1 is saved.")
        String name,

        @Schema(description = "T2 UUID or CM- code. Flow A Add to Catalog.")
        String commercialMasterId,

        String commercialMasterCode,

        @Schema(description = "T1 UUID or SCC- code. Flow B when T2 is missing.")
        String scientificMasterId,

        String scientificMasterCode,

        String assay,
        String grade,
        String form,
        String origin,
        String colour,
        String source,
        Boolean holdPublish) {
}
