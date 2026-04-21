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
package org.apache.fineract.infrastructure.core.service;

import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.configuration.data.SMTPCredentialsData;
import org.apache.fineract.infrastructure.configuration.service.ExternalServicesPropertiesReadPlatformService;
import org.apache.fineract.infrastructure.core.domain.EmailDetail;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SelfServicePluginEmailServiceImpl implements SelfServicePluginEmailService {

    private final ExternalServicesPropertiesReadPlatformService externalServicesReadPlatformService;
    private final Environment env;

    private final AtomicBoolean smtpFallbackLogged = new AtomicBoolean(false);

    public SelfServicePluginEmailServiceImpl(
            ExternalServicesPropertiesReadPlatformService externalServicesReadPlatformService,
            Environment env) {
        this.externalServicesReadPlatformService = externalServicesReadPlatformService;
        this.env = env;
    }

    @Override
    public void sendFormattedEmail(EmailDetail emailDetails) {
        final SMTPCredentialsData smtpCredentialsData;
        try {
            smtpCredentialsData = resolveSmtpCredentials();
        } catch (SmtpConfigurationUnavailableException e) {
            throw new PlatformEmailSendException(e);
        }
        final JavaMailSenderImpl mailSender = configureMailSender(smtpCredentialsData);
        try {
            final MimeMessage mimeMessage = mailSender.createMimeMessage();
            final MimeMessageHelper message = new MimeMessageHelper(mimeMessage, "UTF-8");
            message.setFrom(smtpCredentialsData.getFromEmail());
            message.setTo(emailDetails.getAddress());
            message.setSubject(emailDetails.getSubject());
            message.setText(emailDetails.getBody(), true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            throw new PlatformEmailSendException(e);
        }
    }

    SMTPCredentialsData resolveSmtpCredentials() {
        try {
            return this.externalServicesReadPlatformService.getSMTPCredentials();
        } catch (DataAccessException dae) {
            if (smtpFallbackLogged.compareAndSet(false, true)) {
                log.warn("SMTP configuration table unavailable ({}); falling back to Spring properties "
                        + "(fineract.selfservice.smtp.*). Further occurrences will be logged at DEBUG.",
                        dae.getClass().getSimpleName());
            } else {
                log.debug("SMTP configuration table unavailable, using Spring properties fallback.");
            }
            return buildCredentialsFromEnvironment(dae);
        }
    }

    private SMTPCredentialsData buildCredentialsFromEnvironment(DataAccessException originalCause) {
        String host = env.getProperty("fineract.selfservice.smtp.host");
        String fromEmail = env.getProperty("fineract.selfservice.smtp.from-email");

        if (StringUtils.isBlank(host) || StringUtils.isBlank(fromEmail)) {
            throw new SmtpConfigurationUnavailableException(
                    "SMTP configuration unavailable: the Fineract core table 'c_external_service_properties' "
                            + "does not exist and the required Spring properties 'fineract.selfservice.smtp.host' and "
                            + "'fineract.selfservice.smtp.from-email' are not configured.",
                    originalCause);
        }

        String port = env.getProperty("fineract.selfservice.smtp.port", "587");
        String username = env.getProperty("fineract.selfservice.smtp.username", "");
        String password = env.getProperty("fineract.selfservice.smtp.password", "");
        String fromName = env.getProperty("fineract.selfservice.smtp.from-name", "");
        boolean useTls = env.getProperty("fineract.selfservice.smtp.use-tls", Boolean.class, true);

        return new SMTPCredentialsData()
                .setHost(host)
                .setPort(port)
                .setUsername(username)
                .setPassword(password)
                .setFromEmail(fromEmail)
                .setFromName(fromName)
                .setUseTLS(useTls);
    }

    private JavaMailSenderImpl configureMailSender(SMTPCredentialsData smtpCredentialsData) {
        final JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(smtpCredentialsData.getHost());

        String portStr = smtpCredentialsData.getPort();
        int port = 587;
        if (portStr != null && !portStr.isEmpty()) {
            try {
                int parsed = Integer.parseInt(portStr);
                if (parsed >= 1 && parsed <= 65535) {
                    port = parsed;
                } else {
                    log.warn("SMTP port '{}' out of valid range (1-65535), using default 587", portStr);
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid SMTP port '{}', using default 587", portStr);
            }
        }
        mailSender.setPort(port);
        mailSender.setUsername(smtpCredentialsData.getUsername());
        mailSender.setPassword(smtpCredentialsData.getPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        if (StringUtils.isNotBlank(smtpCredentialsData.getUsername())) {
            props.put("mail.smtp.auth", "true");
        }

        String mailDebug = env.getProperty("mail.debug");
        if (mailDebug != null) {
            props.put("mail.debug", mailDebug);
        }

        if (smtpCredentialsData.isUseTLS()) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        return mailSender;
    }
}
