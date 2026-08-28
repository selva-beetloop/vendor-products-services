package com.beetloop.catalog.integration.pm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "beetloop.catalog.pm")
public class PmIntegrationProperties {
    private String baseUrl = "";
    private String serviceToken = "";
    private boolean webhookEnabled = false;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getServiceToken() { return serviceToken; }
    public void setServiceToken(String serviceToken) { this.serviceToken = serviceToken; }
    public boolean isWebhookEnabled() { return webhookEnabled; }
    public void setWebhookEnabled(boolean webhookEnabled) { this.webhookEnabled = webhookEnabled; }

    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isBlank();
    }
}
