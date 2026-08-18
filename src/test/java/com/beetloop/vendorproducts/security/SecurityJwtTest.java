package com.beetloop.vendorproducts.security;

import com.beetloop.vendorproducts.EmbeddedMongoConfig;
import com.beetloop.vendorproducts.VendorProductsServicesApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {VendorProductsServicesApplication.class, EmbeddedMongoConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityJwtTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void catalogRequiresJwt() throws Exception {
        mockMvc.perform(get("/api/vendor/catalog/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void vendorJwtCanReadCatalog() throws Exception {
        mockMvc.perform(get("/api/vendor/catalog/categories")
                        .header("Authorization", JwtTestTokenFactory.bearerToken("vendor-1", List.of("VENDOR"))))
                .andExpect(status().isOk());
    }

    @Test
    void vendorCannotReadVendorQcQueue() throws Exception {
        mockMvc.perform(get("/api/vendor/products/qc-review")
                        .header("Authorization", JwtTestTokenFactory.bearerToken("vendor-1", List.of("VENDOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void vendorQcJwtCanReadVendorQcQueue() throws Exception {
        mockMvc.perform(get("/api/vendor/products/qc-review")
                        .header("Authorization", JwtTestTokenFactory.bearerToken("qc-1", List.of("QC_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void intelJwtCanReadIntelligenceQueue() throws Exception {
        mockMvc.perform(get("/api/vendor/intelligence/qc-review")
                        .header("Authorization", JwtTestTokenFactory.bearerToken("intel-1", List.of("INTEL_QC"))))
                .andExpect(status().isOk());
    }
}
