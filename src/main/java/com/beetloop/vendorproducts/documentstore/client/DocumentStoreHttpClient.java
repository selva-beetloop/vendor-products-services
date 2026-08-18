package com.beetloop.vendorproducts.documentstore.client;

import com.beetloop.vendorproducts.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * Binary view/download (and a RestClient fallback for upload) against documents-store.
 * JWT is copied from the inbound request the same way Feign token propagation works.
 */
@Component
@ConditionalOnProperty(name = "app.document-store.enabled", havingValue = "true", matchIfMissing = true)
public class DocumentStoreHttpClient {

    private static final String STORE = "/document/api/documents/store";

    private final RestClient restClient;

    public DocumentStoreHttpClient(@Value("${app.document-store.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public Resource view(String id) {
        return getBytes(STORE + "/view/{id}", id);
    }

    public Resource download(String id) {
        return getBytes(STORE + "/download/{id}", id);
    }

    public Optional<String> upload(MultipartFile file, String moduleReference, String referenceId, String documentType) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());
        body.add("moduleReference", moduleReference);
        body.add("referenceId", referenceId);
        body.add("documentType", documentType);
        try {
            return Optional.ofNullable(restClient.post()
                    .uri(STORE + "/upload")
                    .headers(this::copyBearer)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(String.class));
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("Document-store upload failed: " + ex.getStatusCode(), ex);
        }
    }

    private Resource getBytes(String path, String id) {
        try {
            ResponseEntity<byte[]> entity = restClient.get()
                    .uri(path, id)
                    .headers(this::copyBearer)
                    .retrieve()
                    .onStatus(status -> status.value() == 404,
                            (req, res) -> {
                                throw ResourceNotFoundException.file(id);
                            })
                    .toEntity(byte[].class);
            byte[] body = entity.getBody();
            if (body == null) {
                throw ResourceNotFoundException.file(id);
            }
            return new ByteArrayResource(body);
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw ResourceNotFoundException.file(id);
            }
            throw new IllegalStateException("Document-store fetch failed: " + ex.getStatusCode(), ex);
        }
    }

    private void copyBearer(HttpHeaders headers) {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servlet) {
            String authorization = servlet.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null && !authorization.isBlank()) {
                headers.set(HttpHeaders.AUTHORIZATION, authorization);
            }
        }
    }
}
