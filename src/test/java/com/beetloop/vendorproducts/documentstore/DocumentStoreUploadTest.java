package com.beetloop.vendorproducts.documentstore;

import com.beetloop.vendorproducts.EmbeddedMongoConfig;
import com.beetloop.vendorproducts.VendorProductsServicesApplication;
import com.beetloop.vendorproducts.security.JwtTestTokenFactory;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {VendorProductsServicesApplication.class, EmbeddedMongoConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentStoreUploadTest {

    private static final WireMockServer DOCUMENT_STORE = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        DOCUMENT_STORE.start();
    }

    @DynamicPropertySource
    static void documentStoreUrl(DynamicPropertyRegistry registry) {
        registry.add("app.document-store.enabled", () -> "true");
        registry.add("app.document-store.base-url",
                () -> "http://localhost:" + DOCUMENT_STORE.port() + "/document-store");
    }

    @AfterAll
    static void stopWireMock() {
        DOCUMENT_STORE.stop();
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void uploadAndDownloadProxyToDocumentStore() throws Exception {
        DOCUMENT_STORE.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(
                        urlPathEqualTo("/document-store/document/api/documents/store/upload"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id":"doc-mongo-1","fileName":"coa.pdf","fileType":"application/pdf","fileSizeBytes":18}
                                """)));
        DOCUMENT_STORE.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(
                        urlPathEqualTo("/document-store/document/api/documents/store/view/doc-mongo-1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/pdf")
                        .withBody("%PDF-1.4 fixture")));

        String token = JwtTestTokenFactory.bearerToken("vendor-1", List.of("VENDOR"));
        mockMvc.perform(multipart("/api/vendor/uploads")
                        .file(new MockMultipartFile("file", "coa.pdf", MediaType.APPLICATION_PDF_VALUE,
                                "%PDF-1.4 fixture".getBytes()))
                        .param("module", "product-document")
                        .header("Authorization", token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("doc-mongo-1"))
                .andExpect(jsonPath("$.fileName").value("coa.pdf"))
                .andExpect(jsonPath("$.url").value("/api/vendor/uploads/doc-mongo-1"));

        mockMvc.perform(get("/api/vendor/uploads/doc-mongo-1")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PDF")));
    }
}
