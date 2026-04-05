/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.fineract.selfservice.products.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.portfolio.client.exception.ClientNotFoundException;
import org.apache.fineract.portfolio.savings.data.SavingsProductData;
import org.apache.fineract.portfolio.savings.service.SavingsProductReadPlatformService;
import org.apache.fineract.selfservice.client.service.AppSelfServiceUserClientMapperReadService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class SelfSavingsProductsApiResourceTest {

  @Mock private SavingsProductReadPlatformService savingsProductReadPlatformService;
  @Mock private DefaultToApiJsonSerializer<SavingsProductData> toApiJsonSerializer;
  @Mock private ApiRequestParameterHelper apiRequestParameterHelper;
  @Mock private AppSelfServiceUserClientMapperReadService appUserClientMapperReadService;
  @Mock private UriInfo uriInfo;

  private SelfSavingsProductsApiResource resource;

  private static final Long CLIENT_ID = 5L;
  private static final Long PRODUCT_ID = 1L;

  @BeforeEach
  void setUp() {
    resource = new SelfSavingsProductsApiResource(
        savingsProductReadPlatformService,
        toApiJsonSerializer,
        apiRequestParameterHelper,
        appUserClientMapperReadService);

    Mockito.lenient().when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
    Mockito.lenient().when(apiRequestParameterHelper.process(any()))
        .thenReturn(mock(ApiRequestJsonSerializationSettings.class));
  }

  @Test
  void retrieveAll_validClient_callsReadService() {
    Collection<SavingsProductData> products = List.of(mock(SavingsProductData.class));
    when(savingsProductReadPlatformService.retrieveAll()).thenReturn(products);
    when(toApiJsonSerializer.serialize(any(), any(Collection.class))).thenReturn("[]");

    String result = resource.retrieveAll(CLIENT_ID, uriInfo);

    assertNotNull(result);
    verify(savingsProductReadPlatformService).retrieveAll();
  }

  @Test
  void retrieveAll_callsValidateMapping() {
    when(savingsProductReadPlatformService.retrieveAll()).thenReturn(List.of());
    when(toApiJsonSerializer.serialize(any(), any(Collection.class))).thenReturn("[]");

    resource.retrieveAll(CLIENT_ID, uriInfo);

    verify(appUserClientMapperReadService).validateAppSelfServiceUserClientsMapping(CLIENT_ID);
  }

  @Test
  void retrieveOne_validClient_callsReadService() {
    SavingsProductData product = mock(SavingsProductData.class);
    when(savingsProductReadPlatformService.retrieveOne(PRODUCT_ID)).thenReturn(product);
    when(toApiJsonSerializer.serialize(any(), any(SavingsProductData.class))).thenReturn("{}");

    String result = resource.retrieveOne(PRODUCT_ID, CLIENT_ID, uriInfo);

    assertNotNull(result);
    verify(savingsProductReadPlatformService).retrieveOne(PRODUCT_ID);
  }

  @Test
  void retrieveOne_unmappedClient_throws() {
    doThrow(new ClientNotFoundException(CLIENT_ID))
        .when(appUserClientMapperReadService).validateAppSelfServiceUserClientsMapping(CLIENT_ID);

    Assertions.assertThrows(
        ClientNotFoundException.class,
        () -> resource.retrieveOne(PRODUCT_ID, CLIENT_ID, uriInfo));
  }

  @Test
  void noCoreApiResourceInjected() {
    boolean hasCoreApiResource = Arrays.stream(SelfSavingsProductsApiResource.class.getDeclaredFields())
        .map(Field::getType)
        .anyMatch(t -> t.getSimpleName().equals("SavingsProductsApiResource"));
    assertTrue(!hasCoreApiResource,
        "SelfSavingsProductsApiResource must not depend on core SavingsProductsApiResource");
  }
}
