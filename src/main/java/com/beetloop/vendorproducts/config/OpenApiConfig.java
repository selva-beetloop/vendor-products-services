package com.beetloop.vendorproducts.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI vendorProductsOpenApi() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .info(new Info()
                        .title("Vendor Products & Services API")
                        .version("1.0.0")
                        .description("""
                                Backend for the Beetloop vendor portal's **Products** tab
                                (`/vendor/products-services`).

                                Covers all five categories — Raw Materials, Processing Machinery,
                                Finished Goods, Packaging Materials, Packaging Machinery — and both
                                save modes:

                                * **Step-based save** — `PUT /products/{id}/identity`,
                                  `PUT /products/{id}/role`, `POST|PUT /products/{id}/variants[/{variantId}]`
                                  and the per-sub-step variant endpoints. Each validates only its own
                                  step and preserves everything saved earlier.
                                * **Overall save** — `POST /products/{id}/save` writes every section in
                                  one transaction, then `POST /products/{id}/submit` moves the product
                                  to QC.

                                Category-specific fields for Step 1 and Step 2 are declared in
                                `category-schemas.json` and published by `GET /catalog/categories`;
                                validation errors come back keyed by the same field names the wizard
                                uses in its form state.
                                """)
                        .license(new License().name("Proprietary")))
                .servers(List.of(
                        new Server().url("http://localhost:8086/vendor-products")
                                .description("Local development")));
    }
}
