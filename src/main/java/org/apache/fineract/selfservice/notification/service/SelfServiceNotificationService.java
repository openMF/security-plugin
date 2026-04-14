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
package org.apache.fineract.selfservice.notification.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.campaigns.sms.data.SmsProviderData;
import org.apache.fineract.infrastructure.campaigns.sms.service.SmsCampaignDropdownReadPlatformService;
import org.apache.fineract.infrastructure.core.domain.EmailDetail;
import org.apache.fineract.infrastructure.core.service.PlatformEmailService;
import org.apache.fineract.infrastructure.sms.domain.SmsMessage;
import org.apache.fineract.infrastructure.sms.domain.SmsMessageRepository;
import org.apache.fineract.infrastructure.sms.domain.SmsMessageStatusType;
import org.apache.fineract.infrastructure.sms.scheduler.SmsMessageScheduledJobService;
import org.apache.fineract.selfservice.notification.NotificationCooldownCache;
import org.apache.fineract.selfservice.notification.SelfServiceNotificationEvent;
import org.springframework.context.MessageSource;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class SelfServiceNotificationService {

    private final ITemplateEngine notificationTemplateEngine;
    private final MessageSource notificationMessageSource;
    private final PlatformEmailService emailService;
    private final SmsMessageRepository smsMessageRepository;
    private final SmsMessageScheduledJobService smsScheduledJobService;
    private final SmsCampaignDropdownReadPlatformService smsProviderService;
    private final NotificationCooldownCache notificationCooldownCache;
    private final Environment env;

    /**
     * Handles self-service notification events asynchronously.
     *
     * <p>Sends email or SMS notifications based on event configuration, user preferences,
     * and global/per-event feature flags. Events are subject to a per-user cooldown to
     * avoid duplicate notifications in rapid succession.
     *
     * <p>This method executes on the {@code notificationExecutor} thread pool and
     * returns immediately to the caller. Any exception during processing is logged
     * but does not propagate.
     *
     * @param event the notification event containing user details and notification type
     */
    @Async("notificationExecutor")
    @EventListener
    public void handleNotification(SelfServiceNotificationEvent event) {
        try {
            boolean globalEnabled = env.getProperty("fineract.selfservice.notification.enabled", Boolean.class, true);
            if (!globalEnabled) {
                return;
            }

            boolean eventEnabled = env.getProperty("fineract.selfservice.notification." + event.getType().getTemplatePrefix() + ".enabled", Boolean.class, event.getType() != SelfServiceNotificationEvent.Type.LOGIN_FAILURE);
            if (!eventEnabled) {
                return;
            }

            String cacheKey = event.getType().name() + ":" + event.getUserId();
            if (!notificationCooldownCache.tryAcquire(cacheKey)) {
                log.debug("Skipping notification for event type {} and user ID {} due to cooldown.", event.getType(), event.getUserId());
                return;
            }

            Context context = new Context(event.getLocale() != null ? event.getLocale() : java.util.Locale.getDefault());
            context.setVariable("firstName", event.getFirstName());
            context.setVariable("lastName", event.getLastName());
            context.setVariable("username", event.getUsername());
            if (event.getIpAddress() != null) {
                context.setVariable("ipAddress", event.getIpAddress());
            }
            context.setVariable("eventTimestamp", java.time.ZonedDateTime.now());

            String subjectKey = "subject." + event.getType().getTemplatePrefix();
            String subject = notificationMessageSource.getMessage(subjectKey, null, subjectKey, context.getLocale());

            if (event.isEmailMode()) {
                if (org.apache.commons.lang3.StringUtils.isBlank(event.getEmail())) {
                    log.warn("Email notification skipped for event {} because no email address is available", event.getType());
                    return;
                }
                String templateName = "html/" + event.getType().getTemplatePrefix();
                String htmlBody = notificationTemplateEngine.process(templateName, context);
                
                String recipientName = buildRecipientName(event.getFirstName(), event.getLastName());
                EmailDetail emailDetail = new EmailDetail(subject, htmlBody, event.getEmail(), recipientName);
                emailService.sendDefinedEmail(emailDetail);
            } else {
                if (org.apache.commons.lang3.StringUtils.isBlank(event.getMobileNumber())) {
                    log.warn("SMS notification skipped for event {} because no mobile number is available", event.getType());
                    return;
                }
                Collection<SmsProviderData> providers = smsProviderService.retrieveSmsProviders();
                if (providers == null || providers.isEmpty()) {
                    log.warn("No SMS provider configured, SMS notification skipped for event {}", event.getType());
                    return;
                }
                Long providerId = providers.iterator().next().getId();
                
                String templateName = "text/" + event.getType().getTemplatePrefix();
                String textBody = notificationTemplateEngine.process(templateName, context);
                
                SmsMessage smsMessage = SmsMessage.instance(null, null, null, null, SmsMessageStatusType.PENDING, textBody, event.getMobileNumber(), null, true);
                smsMessageRepository.save(smsMessage);
                smsScheduledJobService.sendTriggeredMessage(new ArrayList<>(List.of(smsMessage)), providerId);
            }
        } catch (Exception e) {
            String cacheKey = event.getType().name() + ":" + event.getUserId();
            notificationCooldownCache.release(cacheKey);
            log.error("Failed to handle notification for event type {}", event.getType(), e);
        }
    }

    private String buildRecipientName(String firstName, String lastName) {
        StringBuilder name = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) {
            name.append(firstName);
        }
        if (lastName != null && !lastName.isBlank()) {
            if (name.length() > 0) {
                name.append(" ");
            }
            name.append(lastName);
        }
        return name.length() > 0 ? name.toString() : "User";
    }
}
