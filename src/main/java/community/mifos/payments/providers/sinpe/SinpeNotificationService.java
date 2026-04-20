/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.providers.sinpe;

import community.mifos.payments.core.domain.PaymentTransaction;
import community.mifos.payments.providers.pix.PixAuditLogger;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

/**
 * Handles SMS and push notifications for SINPE Móvil transactions.
 * Integrates with BCCR notification APIs or external SMS gateways.
 */
@Service
public class SinpeNotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(SinpeNotificationService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#,##0.00");

    private final SinpeConfig config;
    private final RestTemplate restTemplate;
    private final PixAuditLogger auditLogger; // Reused from PIX or create generic audit logger

    public SinpeNotificationService(SinpeConfig config, PixAuditLogger auditLogger) {
        this.config = config;
        this.auditLogger = auditLogger;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Sends SMS confirmation to the recipient after successful SINPE Móvil transfer.
     * Async execution ensures payment flow is never blocked.
     */
    @Async("auditExecutor")
    public void sendConfirmationSms(PaymentTransaction transaction) {
        if (!Boolean.TRUE.equals(config.getSmsNotificationEnabled())) {
            LOG.debug("SMS notifications disabled for SINPE");
            return;
        }

        String recipientPhone = transaction.getRecipientIdentifier();
        if (!isValidPhoneNumber(recipientPhone)) {
            LOG.warn("Invalid recipient phone number for SMS notification: {}", maskPhone(recipientPhone));
            return;
        }

        try {
            String message = buildSmsMessage(transaction);

            // Option 1: BCCR SINPE notification API
            // sendViaBccrApi(recipientPhone, message);

            // Option 2: External SMS gateway (e.g., Twilio, AWS SNS, local provider)
            sendViaSmsGateway(recipientPhone, message);

            LOG.info("SINPE SMS notification sent to {}", maskPhone(recipientPhone));

            auditLogger.logSecurityEvent(
                transaction.getTransactionId(),
                "SINPE",
                "SMS_NOTIFICATION_SENT",
                "SMS confirmation sent to " + maskPhone(recipientPhone),
                null
            );

        } catch (Exception ex) {
            // Fail-safe: log error but don't fail the transaction
            LOG.error("Failed to send SINPE SMS notification to {}: {}", 
                maskPhone(recipientPhone), ex.getMessage());

            auditLogger.logSecurityEvent(
                transaction.getTransactionId(),
                "SINPE",
                "SMS_NOTIFICATION_FAILED",
                "Failed to send SMS: " + ex.getMessage(),
                null
            );
        }
    }

    /**
     * Sends SMS notification to the sender (debit confirmation).
     */
    @Async("auditExecutor")
    public void sendDebitConfirmationSms(PaymentTransaction transaction, String senderPhone) {
        if (!Boolean.TRUE.equals(config.getSmsNotificationEnabled())) {
            return;
        }

        if (!isValidPhoneNumber(senderPhone)) {
            return;
        }

        try {
            String message = String.format(
                "SINPE Móvil: Ha enviado %s CRC a %s el %s. Comprobante: %s",
                AMOUNT_FORMAT.format(transaction.getAmount()),
                maskName(transaction.getRecipientName()),
                transaction.getCreatedAtLocal() != null 
                    ? transaction.getCreatedAtLocal().format(DATE_FORMATTER) 
                    : "N/A",
                transaction.getReferenceCode()
            );

            sendViaSmsGateway(senderPhone, message);

            LOG.info("SINPE debit SMS notification sent to sender {}", maskPhone(senderPhone));

        } catch (Exception ex) {
            LOG.error("Failed to send SINPE debit SMS: {}", ex.getMessage());
        }
    }

    /**
     * Sends low-balance warning to sender if balance drops below threshold.
     */
    @Async("auditExecutor")
    public void sendLowBalanceAlert(String phoneNumber, BigDecimal currentBalance, BigDecimal threshold) {
        if (!Boolean.TRUE.equals(config.getSmsNotificationEnabled())) {
            return;
        }

        try {
            String message = String.format(
                "SINPE Móvil: Su saldo es de %s CRC, por debajo del límite de %s CRC. " +
                "Recuerde recargar su cuenta.",
                AMOUNT_FORMAT.format(currentBalance),
                AMOUNT_FORMAT.format(threshold)
            );

            sendViaSmsGateway(phoneNumber, message);

        } catch (Exception ex) {
            LOG.error("Failed to send low balance alert: {}", ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Private Helpers
    // -------------------------------------------------------------------------

    private String buildSmsMessage(PaymentTransaction transaction) {
        return String.format(
            "SINPE Móvil: Ha recibido %s CRC de %s el %s. " +
            "Comprobante: %s. Si no reconoce esta transacción, llame al 800-SINPE.",
            AMOUNT_FORMAT.format(transaction.getAmount()),
            maskName(transaction.getRecipientName()), // Actually sender name, but stored in recipient context for incoming
            transaction.getCreatedAtLocal() != null 
                ? transaction.getCreatedAtLocal().format(DATE_FORMATTER) 
                : "N/A",
            transaction.getReferenceCode()
        );
    }

    /**
     * Sends SMS via external gateway (Twilio, AWS SNS, or local Costa Rican provider).
     * Placeholder implementation - integrate with actual provider.
     */
    private void sendViaSmsGateway(String phoneNumber, String message) {
        // Example Twilio integration:
        // String url = String.format("https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json", accountSid);
        // MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        // params.add("To", "+506" + phoneNumber);
        // params.add("From", twilioPhoneNumber);
        // params.add("Body", message);
        // restTemplate.postForEntity(url, new HttpEntity<>(params, authHeaders), String.class);

        // Example BCCR SINPE notification API:
        // restTemplate.postForEntity(
        //     config.getBaseUrl() + "/sinpe/v1/notificaciones/sms",
        //     new SinpeSmsRequest(phoneNumber, message),
        //     Void.class
        // );

        LOG.debug("SMS to {}: {}", maskPhone(phoneNumber), message);
    }

    /**
     * Validates Costa Rican phone number (8 digits, starts with 2,5,7,8,9).
     */
    private boolean isValidPhoneNumber(String phone) {
        if (phone == null || phone.length() != 8) {
            return false;
        }
        return phone.matches("^[25789]\\d{7}$");
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return phone.substring(0, 2) + "****" + phone.substring(phone.length() - 2);
    }

    private String maskName(String name) {
        if (name == null || name.length() < 3) return "***";
        return name.charAt(0) + "***" + name.charAt(name.length() - 1);
    }
}