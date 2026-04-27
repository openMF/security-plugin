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
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Component
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