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
package org.apache.fineract.selfservice.client.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.serialization.ToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.portfolio.accountdetails.data.AccountSummaryCollectionData;
import org.apache.fineract.portfolio.accountdetails.service.AccountDetailsReadPlatformService;
import org.apache.fineract.portfolio.client.data.ClientChargeData;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.fineract.portfolio.client.data.ClientTransactionData;
import org.apache.fineract.portfolio.client.exception.ClientNotFoundException;
import org.apache.fineract.portfolio.client.service.ClientChargeReadPlatformService;
import org.apache.fineract.portfolio.client.service.ClientTransactionReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.guarantor.data.ObligeeData;
import org.apache.fineract.portfolio.loanaccount.guarantor.service.GuarantorReadPlatformService;
import org.apache.fineract.selfservice.client.data.SelfClientDataValidator;
import org.apache.fineract.selfservice.client.service.SelfServiceClientReadPlatformService;
import org.apache.fineract.selfservice.client.service.SelfServiceSearchParameters;
import org.apache.fineract.selfservice.security.service.PlatformSelfServiceSecurityContext;
import org.apache.fineract.selfservice.useradministration.domain.AppSelfServiceUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.fineract.selfservice.client.service.AppSelfServiceUserClientMapperReadService;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class SelfClientsApiResourceTest {

  @Mock private PlatformSelfServiceSecurityContext context;
  @Mock private SelfServiceClientReadPlatformService selfServiceClientReadPlatformService;
  @Mock private AccountDetailsReadPlatformService accountDetailsReadPlatformService;
  @Mock private ClientChargeReadPlatformService clientChargeReadPlatformService;
  @Mock private ClientTransactionReadPlatformService clientTransactionReadPlatformService;
  @Mock private GuarantorReadPlatformService guarantorReadPlatformService;
  @Mock private ToApiJsonSerializer<ClientData> clientSerializer;
  @Mock private ToApiJsonSerializer<AccountSummaryCollectionData> accountSummarySerializer;
  @Mock private DefaultToApiJsonSerializer<ClientChargeData> clientChargeSerializer;
  @Mock private DefaultToApiJsonSerializer<ClientTransactionData> clientTransactionSerializer;
  @Mock private DefaultToApiJsonSerializer<ObligeeData> obligeeSerializer;
  @Mock private ApiRequestParameterHelper apiRequestParameterHelper;
  @Mock private AppSelfServiceUserClientMapperReadService appUserClientMapperReadService;
  @Mock private SelfClientDataValidator dataValidator;
  @Mock private UriInfo uriInfo;

  private SelfClientsApiResource resource;

  private static final Long USER_ID = 10L;
  private static final Long CLIENT_ID = 5L;

  @BeforeEach
  void setUp() {
    resource = new SelfClientsApiResource(
        context,
        selfServiceClientReadPlatformService,
        accountDetailsReadPlatformService,
        clientChargeReadPlatformService,
        clientTransactionReadPlatformService,
        guarantorReadPlatformService,
        clientSerializer,
        accountSummarySerializer,
        clientChargeSerializer,
        clientTransactionSerializer,
        obligeeSerializer,
        apiRequestParameterHelper,
        appUserClientMapperReadService,
        dataValidator,
        null, // imageReadPlatformService
        null, // commandPipeline
        null, // imageResizeContentProcessor
        null, // base64EncoderContentProcessor
        null, // base64DecoderContentProcessor
        null, // dataUrlEncoderContentProcessor
        null, // dataUrlDecoderContentProcessor
        null, // sizeContentProcessor
        null  // contentDetectorManager
    );

    org.mockito.Mockito.lenient()
        .when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
    org.mockito.Mockito.lenient()
        .when(apiRequestParameterHelper.process(any()))
        .thenReturn(mock(ApiRequestJsonSerializationSettings.class));
  }

  private void mockAuthenticatedUser() {
    AppSelfServiceUser user = mock(AppSelfServiceUser.class);
    when(user.getId()).thenReturn(USER_ID);
    when(context.authenticatedSelfServiceUser()).thenReturn(user);
  }

  private void mockClientMapped() {
    mockAuthenticatedUser();
    when(appUserClientMapperReadService.isClientMappedToSelfServiceUser(CLIENT_ID, USER_ID)).thenReturn(true);
  }

  private void mockClientNotMapped() {
    mockAuthenticatedUser();
    when(appUserClientMapperReadService.isClientMappedToSelfServiceUser(CLIENT_ID, USER_ID)).thenReturn(false);
  }

  // --- MX-206: retrieveAll ---

  @Test
  void retrieveAll_usesIsSelfUserTrue() {
    Page<ClientData> page = mock(Page.class);
    when(selfServiceClientReadPlatformService.retrieveAll(any(SelfServiceSearchParameters.class)))
        .thenReturn(page);
    when(clientSerializer.serialize(any(), any(Page.class))).thenReturn("[]");

    resource.retrieveAll(uriInfo, null, null, null, null, null, null, null, null, null);

    ArgumentCaptor<SelfServiceSearchParameters> captor =
        ArgumentCaptor.forClass(SelfServiceSearchParameters.class);
    verify(selfServiceClientReadPlatformService).retrieveAll(captor.capture());
    assertNotNull(captor.getValue());
    assert captor.getValue().getIsSelfUser();
  }

  @Test
  void retrieveAll_neverCallsValidateMapping() {
    Page<ClientData> page = mock(Page.class);
    when(selfServiceClientReadPlatformService.retrieveAll(any(SelfServiceSearchParameters.class)))
        .thenReturn(page);
    when(clientSerializer.serialize(any(), any(Page.class))).thenReturn("[]");

    resource.retrieveAll(uriInfo, null, null, null, null, null, null, null, null, null);

    verify(appUserClientMapperReadService, never()).isClientMappedToSelfServiceUser(anyLong(), anyLong());
  }

  // --- MX-207: retrieveOne ---

  @Test
  void retrieveOne_mappedClient_returnsSerializedData() {
    mockClientMapped();
    ClientData clientData = mock(ClientData.class);
    when(selfServiceClientReadPlatformService.retrieveOne(CLIENT_ID)).thenReturn(clientData);
    when(clientSerializer.serialize(any(), eq(clientData))).thenReturn("{\"id\":5}");

    String result = resource.retrieveOne(CLIENT_ID, uriInfo);

    assertNotNull(result);
    verify(selfServiceClientReadPlatformService).retrieveOne(CLIENT_ID);
  }

  @Test
  void retrieveOne_unmappedClient_throws() {
    mockClientNotMapped();

    assertThrows(ClientNotFoundException.class, () -> resource.retrieveOne(CLIENT_ID, uriInfo));
  }

  // --- MX-208: retrieveAssociatedAccounts ---

  @Test
  void retrieveAssociatedAccounts_mappedClient_returnsAccounts() {
    mockClientMapped();
    AccountSummaryCollectionData accounts = mock(AccountSummaryCollectionData.class);
    when(accountDetailsReadPlatformService.retrieveClientAccountDetails(CLIENT_ID))
        .thenReturn(accounts);
    when(accountSummarySerializer.serialize(any(), eq(accounts))).thenReturn("{}");

    String result = resource.retrieveAssociatedAccounts(CLIENT_ID, uriInfo);

    assertNotNull(result);
    verify(accountDetailsReadPlatformService).retrieveClientAccountDetails(CLIENT_ID);
  }

  @Test
  void retrieveAssociatedAccounts_unmappedClient_throws() {
    mockClientNotMapped();

    assertThrows(
        ClientNotFoundException.class,
        () -> resource.retrieveAssociatedAccounts(CLIENT_ID, uriInfo));
  }

  // --- MX-210: charges ---

  @Test
  void retrieveAllClientCharges_mappedClient_returnsCharges() {
    mockClientMapped();
    Page<ClientChargeData> page = mock(Page.class);
    when(clientChargeReadPlatformService.retrieveClientCharges(
            eq(CLIENT_ID), any(), any(), any(SearchParameters.class)))
        .thenReturn(page);
    when(clientChargeSerializer.serialize(any(), any(Page.class))).thenReturn("[]");

    String result =
        resource.retrieveAllClientCharges(CLIENT_ID, "all", null, uriInfo, null, null);

    assertNotNull(result);
    verify(clientChargeReadPlatformService)
        .retrieveClientCharges(eq(CLIENT_ID), eq("all"), eq(null), any(SearchParameters.class));
  }

  @Test
  void retrieveAllClientCharges_unmappedClient_throws() {
    mockClientNotMapped();

    assertThrows(
        ClientNotFoundException.class,
        () -> resource.retrieveAllClientCharges(CLIENT_ID, "all", null, uriInfo, null, null));
  }

  @Test
  void retrieveClientCharge_mappedClient_returnsCharge() {
    mockClientMapped();
    ClientChargeData charge = mock(ClientChargeData.class);
    when(clientChargeReadPlatformService.retrieveClientCharge(CLIENT_ID, 1L)).thenReturn(charge);
    when(clientChargeSerializer.serialize(any(), eq(charge))).thenReturn("{}");

    String result = resource.retrieveClientCharge(CLIENT_ID, 1L, uriInfo);

    assertNotNull(result);
    verify(clientChargeReadPlatformService).retrieveClientCharge(CLIENT_ID, 1L);
  }

  // --- MX-211: retrieveAllClientTransactions ---

  @Test
  void retrieveAllClientTransactions_mappedClient_returnsTransactions() {
    mockClientMapped();
    Page<ClientTransactionData> page = mock(Page.class);
    when(clientTransactionReadPlatformService.retrieveAllTransactions(
            eq(CLIENT_ID), any(SearchParameters.class)))
        .thenReturn(page);
    when(clientTransactionSerializer.serialize(any(), any(Page.class))).thenReturn("[]");

    String result = resource.retrieveAllClientTransactions(CLIENT_ID, uriInfo, null, null);

    assertNotNull(result);
    verify(clientTransactionReadPlatformService)
        .retrieveAllTransactions(eq(CLIENT_ID), any(SearchParameters.class));
  }

  @Test
  void retrieveAllClientTransactions_unmappedClient_throws() {
    mockClientNotMapped();

    assertThrows(
        ClientNotFoundException.class,
        () -> resource.retrieveAllClientTransactions(CLIENT_ID, uriInfo, null, null));
  }

  // --- MX-212: retrieveClientTransaction ---

  @Test
  void retrieveClientTransaction_mappedClient_returnsTransaction() {
    mockClientMapped();
    ClientTransactionData txn = mock(ClientTransactionData.class);
    when(clientTransactionReadPlatformService.retrieveTransaction(CLIENT_ID, 99L)).thenReturn(txn);
    when(clientTransactionSerializer.serialize(any(), eq(txn))).thenReturn("{}");

    String result = resource.retrieveClientTransaction(CLIENT_ID, 99L, uriInfo);

    assertNotNull(result);
    verify(clientTransactionReadPlatformService).retrieveTransaction(CLIENT_ID, 99L);
  }

  @Test
  void retrieveClientTransaction_unmappedClient_throws() {
    mockClientNotMapped();

    assertThrows(
        ClientNotFoundException.class,
        () -> resource.retrieveClientTransaction(CLIENT_ID, 99L, uriInfo));
  }

  // --- retrieveObligeeDetails ---

  @Test
  void retrieveObligeeDetails_mappedClient_returnsObligees() {
    mockClientMapped();
    List<ObligeeData> obligees = List.of(mock(ObligeeData.class));
    when(guarantorReadPlatformService.retrieveObligeeDetails(CLIENT_ID)).thenReturn(obligees);
    when(obligeeSerializer.serialize(any(), eq(obligees))).thenReturn("[]");

    String result = resource.retrieveObligeeDetails(CLIENT_ID, uriInfo);

    assertNotNull(result);
    verify(guarantorReadPlatformService).retrieveObligeeDetails(CLIENT_ID);
  }

  @Test
  void retrieveObligeeDetails_unmappedClient_throws() {
    mockClientNotMapped();

    assertThrows(
        ClientNotFoundException.class,
        () -> resource.retrieveObligeeDetails(CLIENT_ID, uriInfo));
  }

  // --- Security contract ---

  @Test
  void validateAppuserClientsMapping_usesAuthenticatedSelfServiceUser() {
    mockClientMapped();
    ClientData clientData = mock(ClientData.class);
    when(selfServiceClientReadPlatformService.retrieveOne(CLIENT_ID)).thenReturn(clientData);
    when(clientSerializer.serialize(any(), eq(clientData))).thenReturn("{}");

    resource.retrieveOne(CLIENT_ID, uriInfo);

    verify(context).authenticatedSelfServiceUser();
  }
}
