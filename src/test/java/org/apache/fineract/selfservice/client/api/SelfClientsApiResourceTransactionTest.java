package org.apache.fineract.selfservice.client.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.portfolio.client.data.ClientTransactionData;
import org.apache.fineract.portfolio.client.exception.ClientNotFoundException;
import org.apache.fineract.portfolio.client.service.ClientTransactionReadPlatformService;
import org.apache.fineract.selfservice.client.service.AppuserClientMapperReadService;
import org.apache.fineract.selfservice.security.service.PlatformSelfServiceSecurityContext;
import org.apache.fineract.selfservice.useradministration.domain.AppSelfServiceUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SelfClientsApiResourceTransactionTest {

  @Mock private PlatformSelfServiceSecurityContext context;
  @Mock private ClientTransactionReadPlatformService clientTransactionReadPlatformService;
  @Mock private DefaultToApiJsonSerializer<ClientTransactionData> clientTransactionSerializer;
  @Mock private ApiRequestParameterHelper apiRequestParameterHelper;
  @Mock private AppuserClientMapperReadService appUserClientMapperReadService;
  @Mock private AppSelfServiceUser selfServiceUser;
  @Mock private UriInfo uriInfo;

  private SelfClientsApiResource resource;

  @BeforeEach
  void setUp() {
    resource =
        new SelfClientsApiResource(
            context,
            null, // clientApiResource
            null, // clientChargesApiResource
            clientTransactionReadPlatformService,
            clientTransactionSerializer,
            apiRequestParameterHelper,
            appUserClientMapperReadService,
            null, // dataValidator
            null, // imageReadPlatformService
            null, // commandPipeline
            null, // imageResizeContentProcessor
            null, // base64EncoderContentProcessor
            null, // base64DecoderContentProcessor
            null, // dataUrlEncoderContentProcessor
            null, // dataUrlDecoderContentProcessor
            null, // sizeContentProcessor
            null); // contentDetectorManager
  }

  @Test
  void retrieveClientTransaction_shouldReturnSerializedTransaction() {
    Long clientId = 104L;
    Long transactionId = 233L;
    String expectedJson = "{\"id\":233}";

    when(context.authenticatedSelfServiceUser()).thenReturn(selfServiceUser);
    when(selfServiceUser.getId()).thenReturn(1L);
    when(appUserClientMapperReadService.isClientMappedToUser(clientId, 1L)).thenReturn(true);
    when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());

    ApiRequestJsonSerializationSettings settings = mock(ApiRequestJsonSerializationSettings.class);
    when(apiRequestParameterHelper.process(any())).thenReturn(settings);

    ClientTransactionData transactionData = mock(ClientTransactionData.class);
    when(clientTransactionReadPlatformService.retrieveTransaction(clientId, transactionId))
        .thenReturn(transactionData);
    when(clientTransactionSerializer.serialize(eq(settings), eq(transactionData)))
        .thenReturn(expectedJson);

    String result = resource.retrieveClientTransaction(clientId, transactionId, uriInfo);

    assertEquals(expectedJson, result);
    verify(clientTransactionReadPlatformService).retrieveTransaction(clientId, transactionId);
  }

  @Test
  void retrieveClientTransaction_shouldThrowWhenClientNotMapped() {
    Long clientId = 999L;
    Long transactionId = 1L;

    when(context.authenticatedSelfServiceUser()).thenReturn(selfServiceUser);
    when(selfServiceUser.getId()).thenReturn(1L);
    when(appUserClientMapperReadService.isClientMappedToUser(clientId, 1L)).thenReturn(false);

    assertThrows(
        ClientNotFoundException.class,
        () -> resource.retrieveClientTransaction(clientId, transactionId, uriInfo));

    verifyNoInteractions(clientTransactionReadPlatformService);
  }

  @Test
  void retrieveAllClientTransactions_shouldReturnSerializedPage() {
    Long clientId = 104L;
    Integer offset = 0;
    Integer limit = 10;
    String expectedJson = "{\"totalFilteredRecords\":1}";

    when(context.authenticatedSelfServiceUser()).thenReturn(selfServiceUser);
    when(selfServiceUser.getId()).thenReturn(1L);
    when(appUserClientMapperReadService.isClientMappedToUser(clientId, 1L)).thenReturn(true);
    when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());

    ApiRequestJsonSerializationSettings settings = mock(ApiRequestJsonSerializationSettings.class);
    when(apiRequestParameterHelper.process(any())).thenReturn(settings);

    @SuppressWarnings("unchecked")
    Page<ClientTransactionData> page = mock(Page.class);
    when(clientTransactionReadPlatformService.retrieveAllTransactions(eq(clientId), any(SearchParameters.class)))
        .thenReturn(page);
    when(clientTransactionSerializer.serialize(eq(settings), eq(page)))
        .thenReturn(expectedJson);

    String result = resource.retrieveAllClientTransactions(clientId, uriInfo, offset, limit);

    assertEquals(expectedJson, result);
    verify(clientTransactionReadPlatformService)
        .retrieveAllTransactions(eq(clientId), any(SearchParameters.class));
  }

  @Test
  void retrieveAllClientTransactions_shouldThrowWhenClientNotMapped() {
    Long clientId = 999L;

    when(context.authenticatedSelfServiceUser()).thenReturn(selfServiceUser);
    when(selfServiceUser.getId()).thenReturn(1L);
    when(appUserClientMapperReadService.isClientMappedToUser(clientId, 1L)).thenReturn(false);

    assertThrows(
        ClientNotFoundException.class,
        () -> resource.retrieveAllClientTransactions(clientId, uriInfo, 0, 10));

    verifyNoInteractions(clientTransactionReadPlatformService);
  }

  @Test
  void validateAppuserClientsMapping_usesAuthenticatedSelfServiceUser() {
    Long clientId = 104L;
    Long transactionId = 1L;

    when(context.authenticatedSelfServiceUser()).thenReturn(selfServiceUser);
    when(selfServiceUser.getId()).thenReturn(42L);
    when(appUserClientMapperReadService.isClientMappedToUser(clientId, 42L)).thenReturn(true);
    when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
    when(apiRequestParameterHelper.process(any()))
        .thenReturn(mock(ApiRequestJsonSerializationSettings.class));
    when(clientTransactionReadPlatformService.retrieveTransaction(anyLong(), anyLong()))
        .thenReturn(mock(ClientTransactionData.class));
    when(clientTransactionSerializer.serialize(any(ApiRequestJsonSerializationSettings.class), any(ClientTransactionData.class)))
        .thenReturn("{}");

    resource.retrieveClientTransaction(clientId, transactionId, uriInfo);

    verify(context).authenticatedSelfServiceUser();
    verify(context, never()).authenticatedUser(any());
    verify(appUserClientMapperReadService).isClientMappedToUser(clientId, 42L);
  }
}
