package com.beetloop.vendorproducts.storage;

import com.beetloop.vendorproducts.EmbeddedMongoConfig;
import com.beetloop.vendorproducts.VendorProductsServicesApplication;
import com.beetloop.vendorproducts.security.JwtTestTokenFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {VendorProductsServicesApplication.class, EmbeddedMongoConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileUploadLocalTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void uploadAndDownloadOnLocalDisk() throws Exception {
        String token = JwtTestTokenFactory.bearerToken("vendor-1", List.of("VENDOR"));
        String body = mockMvc.perform(multipart("/api/vendor/uploads")
                        .file(new MockMultipartFile("file", "coa.pdf", MediaType.APPLICATION_PDF_VALUE,
                                "%PDF-1.4 local".getBytes()))
                        .param("module", "product-document")
                        .header("Authorization", token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("coa.pdf"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = body.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
        mockMvc.perform(get("/api/vendor/uploads/" + id).header("Authorization", token))
                .andExpect(status().isOk());
    }
}
