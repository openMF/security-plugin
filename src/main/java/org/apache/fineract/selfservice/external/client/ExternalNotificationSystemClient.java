package org.apache.fineract.selfservice.external.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.configuration.data.NotificationCredentialsData;
import org.apache.fineract.infrastructure.configuration.service.ExternalApiRestServicesPropertiesReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Async;

/**
 * HTTP client for the external SMS gateway.
 *
 * <p>This bean is only created when the external SMS system is explicitly enabled via
 * {@code mifos.self.service.external.sms.system.enabled=true}. Without this guard, the
 * mandatory {@code @Value} properties ({@code url}, {@code header}, {@code token}) would crash
 * the entire application context on startup when the properties aren't configured.
 */
@Slf4j
@Component
@ConditionalOnProperty(
    name = "mifos.self.service.external.sms.system.enabled",
    havingValue = "true",
    matchIfMissing = false)
public class ExternalNotificationSystemClient {
    
    private final ExternalApiRestServicesPropertiesReadPlatformService externalApiRestServicesPropertiesReadPlatformService;
    
    @Autowired
    public ExternalNotificationSystemClient(final ExternalApiRestServicesPropertiesReadPlatformService externalApiRestServicesPropertiesReadPlatformService) {
        this.externalApiRestServicesPropertiesReadPlatformService = externalApiRestServicesPropertiesReadPlatformService;
    
    }
    
    // Kept static as per original code, assuming a simple RestTemplate configuration is sufficient
    private static final RestTemplate restTemplate = new RestTemplate(); 

    @Async
    public CompletableFuture<Void> sendPostRequest(Object requestBody) {
        try {
            NotificationCredentialsData notificationCredentialsData = resolveNotificationCredentials();

            String url = notificationCredentialsData.getHost();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(notificationCredentialsData.getHeader(), notificationCredentialsData.getHeaderValue());

            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = ow.writeValueAsString(requestBody);

            HttpEntity<String> entity = new HttpEntity<>(json, headers);

            restTemplate.exchange(URI.create(url), HttpMethod.POST, entity, JsonNode.class);

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("Error sending notification", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public NotificationCredentialsData resolveNotificationCredentials() {
        NotificationCredentialsData notificationCredentialsData = new NotificationCredentialsData();
        try {
            notificationCredentialsData = this.externalApiRestServicesPropertiesReadPlatformService.getNotificationCredentials();
        } 
        catch (DataAccessException dae) {
            log.warn("National Id Service configuration unavailable, falling back to Spring properties ");
        }
        return notificationCredentialsData;
    }
    
}