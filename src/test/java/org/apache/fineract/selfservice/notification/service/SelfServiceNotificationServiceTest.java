package org.apache.fineract.selfservice.notification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;

import java.util.Collections;
import java.util.Locale;
import org.apache.fineract.infrastructure.campaigns.sms.data.SmsProviderData;
import org.apache.fineract.infrastructure.campaigns.sms.service.SmsCampaignDropdownReadPlatformService;
import org.apache.fineract.infrastructure.core.domain.EmailDetail;
import org.apache.fineract.infrastructure.core.service.PlatformEmailService;
import org.apache.fineract.infrastructure.sms.domain.SmsMessage;
import org.apache.fineract.infrastructure.sms.domain.SmsMessageRepository;
import org.apache.fineract.infrastructure.sms.scheduler.SmsMessageScheduledJobService;
import org.apache.fineract.selfservice.notification.NotificationCooldownCache;
import org.apache.fineract.selfservice.notification.SelfServiceNotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.AfterEach;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

@ExtendWith(MockitoExtension.class)
class SelfServiceNotificationServiceTest {

    @Mock private ITemplateEngine templateEngine;
    @Mock private MessageSource messageSource;
    @Mock private PlatformEmailService emailService;
    @Mock private SmsMessageRepository smsMessageRepository;
    @Mock private SmsMessageScheduledJobService smsScheduledJobService;
    @Mock private SmsCampaignDropdownReadPlatformService smsProviderService;
    @Mock private NotificationCooldownCache cooldownCache;
    @Mock private Environment env;

    private SelfServiceNotificationService service;

    @BeforeEach
    void setUp() {
        org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil.setTenant(new org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant(1L, "default", "Default", "timezone", null));
        java.util.HashMap<org.apache.fineract.infrastructure.businessdate.domain.BusinessDateType, java.time.LocalDate> dates = new java.util.HashMap<>();
        dates.put(org.apache.fineract.infrastructure.businessdate.domain.BusinessDateType.BUSINESS_DATE, java.time.LocalDate.now());
        org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil.setBusinessDates(dates);

        service = new SelfServiceNotificationService(templateEngine, messageSource, emailService,
                smsMessageRepository, smsScheduledJobService, smsProviderService, cooldownCache, env);
    }

    @AfterEach
    void tearDown() {
        org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil.reset();
    }

    @Test
    void handleNotification_EmailMode_SendsEmail() {
        when(env.getProperty(eq("fineract.selfservice.notification.enabled"), eq(Boolean.class), any(Boolean.class))).thenReturn(true);
        when(env.getProperty(eq("fineract.selfservice.notification.login-success.enabled"), eq(Boolean.class), any(Boolean.class))).thenReturn(true);
        when(cooldownCache.tryAcquire(any())).thenReturn(true);
        when(templateEngine.process(eq("html/login-success"), any())).thenReturn("<html>body</html>");
        when(messageSource.getMessage(eq("subject.login-success"), eq(null), eq("subject.login-success"), any(Locale.class))).thenReturn("New Login");

        SelfServiceNotificationEvent event = new SelfServiceNotificationEvent(this,
                SelfServiceNotificationEvent.Type.LOGIN_SUCCESS, 1L, "Test", "User", "testuser",
                "test@example.com", "1234567890", true, "127.0.0.1", Locale.US);

        service.handleNotification(event);

        verify(emailService).sendDefinedEmail(any(EmailDetail.class));
        verify(smsMessageRepository, never()).save(any());
    }

    @Test
    void handleNotification_SmsMode_SkipsIfNoProvider() {
        when(env.getProperty(eq("fineract.selfservice.notification.enabled"), eq(Boolean.class), any(Boolean.class))).thenReturn(true);
        when(env.getProperty(eq("fineract.selfservice.notification.login-success.enabled"), eq(Boolean.class), any(Boolean.class))).thenReturn(true);
        when(cooldownCache.tryAcquire(any())).thenReturn(true);
        
        when(smsProviderService.retrieveSmsProviders()).thenReturn(Collections.emptyList());

        SelfServiceNotificationEvent event = new SelfServiceNotificationEvent(this,
                SelfServiceNotificationEvent.Type.LOGIN_SUCCESS, 1L, "Test", "User", "testuser",
                "test@example.com", "1234567890", false, "127.0.0.1", Locale.US);

        service.handleNotification(event);

        verify(smsProviderService).retrieveSmsProviders();
        verify(smsMessageRepository, never()).save(any());
    }

    @Test
    void handleNotification_SmsMode_SendsSms() {
        when(env.getProperty(eq("fineract.selfservice.notification.enabled"), eq(Boolean.class), any(Boolean.class))).thenReturn(true);
        when(env.getProperty(eq("fineract.selfservice.notification.login-success.enabled"), eq(Boolean.class), any(Boolean.class))).thenReturn(true);
        when(cooldownCache.tryAcquire(any())).thenReturn(true);
        
        SmsProviderData provider = mock(SmsProviderData.class);
        when(provider.getId()).thenReturn(5L);
        when(smsProviderService.retrieveSmsProviders()).thenReturn(Collections.singletonList(provider));
        when(templateEngine.process(eq("text/login-success"), any())).thenReturn("Hello text body");
        
        SelfServiceNotificationEvent event = new SelfServiceNotificationEvent(this,
                SelfServiceNotificationEvent.Type.LOGIN_SUCCESS, 1L, "Test", "User", "testuser",
                "test@example.com", "1234567890", false, "127.0.0.1", Locale.US);

        service.handleNotification(event);

        verify(smsMessageRepository).save(any(SmsMessage.class));
        verify(smsScheduledJobService).sendTriggeredMessage(any(), eq(5L));
    }
}
