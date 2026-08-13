package com.beetloop.catalog.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI catalogOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Beetloop Vendor Catalog API")
                        .version("1.0.0")
                        .description("""
                                Vendor Products & Services catalog.

                                Two save models exist side by side:
                                  * step-wise  - PUT /vendor/{products|services}/{id}/steps/{stepKey}
                                                 merges one step, siblings untouched
                                  * overall    - POST /vendor/{products|services}/save-all
                                                 PUT  /vendor/{products|services}/{id}/save-all
                                                 replaces every step AND the whole child collection

                                Neither ever changes qcStatus. Publishing requires submit-qc plus a QC approval.
                                """))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
