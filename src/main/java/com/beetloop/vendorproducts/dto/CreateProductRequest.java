package com.beetloop.vendorproducts.dto;

import com.beetloop.vendorproducts.domain.ProductCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * {@code POST /products} — opens a draft as soon as the vendor picks a category
 * card on the "List a Product" screen.
 */
@Schema(description = "Creates an empty draft product for one of the five categories.")
public record CreateProductRequest(

        @NotNull(message = "Category is required")
        @Schema(example = "raw-materials",
                allowableValues = {"raw-materials", "processing-machinery", "finished-goods",
                        "packaging-materials", "packaging-machinery"})
        ProductCategory category,

        @Schema(description = "Type card chosen inside Step 1, when the category has a type selector.",
                example = "botanical-extract")
        String identityType,

        @Schema(description = "Id of the master-catalog record the vendor started from, if any.",
                example = "curcumin-95")
        String sourceMasterId,

        @Schema(description = "Working name shown on the catalog card until Step 1 is saved.")
        String name) {
}
