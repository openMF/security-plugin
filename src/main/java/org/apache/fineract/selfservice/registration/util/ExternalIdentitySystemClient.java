package org.apache.fineract.selfservice.registration.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalIdentitySystemClient.class);

    @Value("${mifos.self.service.external.identity.system.url}")
    private String externalIdentitySystemUrl;
    
    
    @Value("${mifos.self.service.external.identity.system.header}")
    private String externalIdentitySystemHeader;
    
    @Value("${mifos.self.service.external.identity.system.token}")
    private String externalIdentitySystemToken;
    
    // Kept static as per original code, assuming a simple RestTemplate configuration is sufficient
    private static final RestTemplate restTemplate = new RestTemplate(); 

    public ResponseEntity<JsonNode> sendPostRequest(Object requestBody) throws Exception {
        
        LOGGER.info("Sending POST request to {} ");
        
        String url = externalIdentitySystemUrl;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(externalIdentitySystemHeader, externalIdentitySystemToken);

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(requestBody);

        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        LOGGER.info("Sending request to {} with body: {}", url, json);

        return restTemplate.exchange(URI.create(url),HttpMethod.POST, entity, JsonNode.class);
        
    }
    
    public ResponseEntity<JsonNode> sendGetRequest(String path) throws Exception {
        
        LOGGER.info("Sending GET request to {} ", path);
        
        String url = externalIdentitySystemUrl + path;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(externalIdentitySystemHeader, externalIdentitySystemToken);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        LOGGER.info("Sending request to {} ", url);

        return restTemplate.exchange(URI.create(url), HttpMethod.GET, entity, JsonNode.class);
    }
    
}