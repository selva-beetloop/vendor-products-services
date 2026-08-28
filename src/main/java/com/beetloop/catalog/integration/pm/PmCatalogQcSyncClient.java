package com.beetloop.catalog.integration.pm;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** BP-07: notify PM catalog QC approval for lot-balance qcStatus sync. */
@Component
public class PmCatalogQcSyncClient {

    private static final Logger log = LoggerFactory.getLogger(PmCatalogQcSyncClient.class);
    private static final String CATALOG_SCOPE = "CATALOG";

    private final PmIntegrationProperties properties;
    private final RestClient restClient;

    public PmCatalogQcSyncClient(PmIntegrationProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    public void notifyQcApproved(String itemCode, String itemName, String vendorId) {
        if (!properties.isWebhookEnabled() || !properties.isConfigured() || itemCode == null || itemCode.isBlank()) {
            return;
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("projectId", CATALOG_SCOPE);
        row.put("orderId", CATALOG_SCOPE);
        row.put("vendorId", vendorId);
        row.put("itemCode", itemCode.trim());
        row.put("ingredientName", itemName);
        row.put("lotNo", itemCode.trim());
        row.put("onHand", BigDecimal.ZERO);
        row.put("reserved", BigDecimal.ZERO);
        row.put("qcStatus", "APPROVED");
        row.put("externalSource", "CATALOG_QC");
        try {
            RestClient.RequestBodySpec spec = restClient.post()
                    .uri(buildUrl("/api/catalog/qc-status-sync"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(row));
            if (properties.getServiceToken() != null && !properties.getServiceToken().isBlank()) {
                spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getServiceToken());
            }
            spec.retrieve().toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("PM catalog QC sync failed for {}: {}", itemCode, ex.getMessage());
        }
    }

    private String buildUrl(String path) {
        String base = properties.getBaseUrl().replaceAll("/+$", "");
        String suffix = path.startsWith("/") ? path : "/" + path;
        return base + suffix;
    }
}
