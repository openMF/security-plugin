package org.apache.fineract.selfservice.external.client;

import com.fasterxml.jackson.databind.JsonNode;
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
public class ExternalIdentitySystemClient {

    @Value("${mifos.self.service.external.identity.system.url}")
    private String externalIdentitySystemUrl;
    
    
    @Value("${mifos.self.service.external.identity.system.header}")
    private String externalIdentitySystemHeader;
    
    @Value("${mifos.self.service.external.identity.system.token}")
    private String externalIdentitySystemToken;
    
    // Kept static as per original code, assuming a simple RestTemplate configuration is sufficient
    private static final RestTemplate restTemplate = new RestTemplate(); 
    
    public ResponseEntity<JsonNode> sendGetRequest(String path) throws Exception {
        
        String url = externalIdentitySystemUrl + path;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(externalIdentitySystemHeader, externalIdentitySystemToken);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        return restTemplate.exchange(URI.create(url), HttpMethod.GET, entity, JsonNode.class);
    }
    
}