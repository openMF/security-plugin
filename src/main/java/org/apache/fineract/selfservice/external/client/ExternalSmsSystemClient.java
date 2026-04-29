package org.apache.fineract.selfservice.external.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

/**
 * HTTP client for the external SMS gateway.
 *
 * <p>This bean is only created when the external SMS system is explicitly enabled via
 * {@code mifos.self.service.external.sms.system.enabled=true}. Without this guard, the
 * mandatory {@code @Value} properties ({@code url}, {@code header}, {@code token}) would crash
 * the entire application context on startup when the properties aren't configured.
 */
@Component
@ConditionalOnProperty(
    name = "mifos.self.service.external.sms.system.enabled",
    havingValue = "true",
    matchIfMissing = false)
public class ExternalSmsSystemClient {

    @Value("${mifos.self.service.external.sms.system.url}")
    private String externalSmsSystemUrl;
        
    @Value("${mifos.self.service.external.sms.system.header}")
    private String externalSmsSystemHeader;
    
    @Value("${mifos.self.service.external.sms.system.token}")
    private String externalSmsSystemToken;
    
    // Kept static as per original code, assuming a simple RestTemplate configuration is sufficient
    private static final RestTemplate restTemplate = new RestTemplate(); 

    public ResponseEntity<JsonNode> sendPostRequest(Object requestBody) throws Exception {
        
        String url = externalSmsSystemUrl;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(externalSmsSystemHeader, externalSmsSystemToken);

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(requestBody);

        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        
        return restTemplate.exchange(URI.create(url),HttpMethod.POST, entity, JsonNode.class);
        
    }
    
}