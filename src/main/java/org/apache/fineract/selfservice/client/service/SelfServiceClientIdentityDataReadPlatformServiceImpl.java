/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.selfservice.client.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.selfservice.registration.api.SelfServiceRetrieveIdentityRequest;
import org.apache.fineract.selfservice.registration.data.PersonIdentityData;
import org.apache.fineract.selfservice.registration.util.ExternalIdentitySystemClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SelfServiceClientIdentityDataReadPlatformServiceImpl implements SelfServiceClientIdentityDataReadPlatformService {

    private final ExternalIdentitySystemClient externalIdentitySystemClient;
    
    @Override
    public PersonIdentityData retrieveClientIdentityData(SelfServiceRetrieveIdentityRequest apiRequestBodyAsJson) throws Exception{
        
        ObjectMapper objectMapper = JsonMapper.builder()
                                            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                                            .build();
        
        ResponseEntity<JsonNode> response = this.externalIdentitySystemClient.sendPostRequest(apiRequestBodyAsJson);        
        
        if (response.getStatusCode() == HttpStatus.OK &&
                response.getBody() != null) {

            JsonNode externalSystemPersonData = response.getBody();
            
            return objectMapper.treeToValue(externalSystemPersonData, PersonIdentityData.class);
            
        } 
        else {
            return new PersonIdentityData();
        }                
    }

}
