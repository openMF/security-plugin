package org.apache.fineract.selfservice.security.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;
import org.apache.fineract.infrastructure.core.serialization.ToApiJsonSerializer;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.selfservice.client.service.SelfServiceClientReadPlatformService;
import org.apache.fineract.selfservice.security.exception.SelfServicePasswordResetRequiredException;
import org.apache.fineract.selfservice.security.service.PlatformSelfServiceSecurityContext;
import org.apache.fineract.selfservice.useradministration.data.AppSelfServiceUserData;
import org.apache.fineract.selfservice.useradministration.domain.AppSelfServiceUser;
import org.apache.fineract.useradministration.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class SelfAuthenticationApiResourceTest {

    @Mock private DaoAuthenticationProvider customAuthenticationProvider;
    @Mock private ToApiJsonSerializer<AppSelfServiceUserData> apiJsonSerializerService;
    @Mock private PlatformSelfServiceSecurityContext springSecurityPlatformSecurityContext;
    @Mock private SelfServiceClientReadPlatformService clientReadPlatformService;

    private SelfAuthenticationApiResource resource;

    @BeforeEach
    void setUp() {
        resource = new SelfAuthenticationApiResource(
            customAuthenticationProvider,
            apiJsonSerializerService,
            springSecurityPlatformSecurityContext,
            clientReadPlatformService
        );
    }

    @Test
    void authenticate_nullBody_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> resource.authenticate(null, true));
        assertThrows(IllegalArgumentException.class, () -> resource.authenticate("null", true));
    }

    @Test
    void authenticate_throwsOnNullUsernameOrPassword() {
        assertThrows(IllegalArgumentException.class, () -> resource.authenticate("{}", true));
        assertThrows(IllegalArgumentException.class, () -> resource.authenticate("{\"username\":\"admin\"}", true));
        assertThrows(IllegalArgumentException.class, () -> resource.authenticate("{\"password\":\"1234\"}", true));
    }

    @Test
    void authenticate_returnsUserDataOnSuccess() {
        String json = "{\"username\":\"admin\", \"password\":\"pass\"}";
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        AppSelfServiceUser principal = mock(AppSelfServiceUser.class);
        Office office = mock(Office.class);
        when(principal.getOffice()).thenReturn(office);
        when(office.getId()).thenReturn(1L);
        when(office.getName()).thenReturn("Head Office");
        when(principal.getId()).thenReturn(100L);
        when(principal.getRoles()).thenReturn(Set.of(mock(Role.class)));
        when(auth.getPrincipal()).thenReturn(principal);
        when(auth.getAuthorities()).thenReturn(Collections.emptyList());

        when(customAuthenticationProvider.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(springSecurityPlatformSecurityContext.doesPasswordHasToBeRenewed(principal)).thenReturn(false);
        when(apiJsonSerializerService.serialize(any())).thenReturn("{}");

        String result = resource.authenticate(json, true);

        assertNotNull(result);
    }

    @Test
    void authenticate_throwsPasswordResetExceptionWhenResetRequired() {
        String json = "{\"username\":\"admin\", \"password\":\"pass\"}";
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        AppSelfServiceUser principal = mock(AppSelfServiceUser.class);
        Office office = mock(Office.class);
        when(principal.getOffice()).thenReturn(office);
        when(principal.getId()).thenReturn(100L);
        when(principal.getRoles()).thenReturn(Set.of(mock(Role.class)));
        when(auth.getPrincipal()).thenReturn(principal);
        when(auth.getAuthorities()).thenReturn(Collections.emptyList());

        when(customAuthenticationProvider.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(springSecurityPlatformSecurityContext.doesPasswordHasToBeRenewed(principal)).thenReturn(true);

        assertThrows(SelfServicePasswordResetRequiredException.class, () -> resource.authenticate(json, true));
    }

}
