/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.providers.sinpe;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Configuration properties for Costa Rica's SINPE (Sistema Nacional de Pagos Electrónicos).
 * Covers both SINPE Móvil (phone-based) and SINPE IBAN transfers.
 * Loaded from application.yml or environment variables.
 */
@ConfigurationProperties(prefix = "payments.providers.sinpe")
@Validated
public class SinpeConfig {

    /**
     * Whether SINPE integration is enabled.
     */
    @NotNull
    private Boolean enabled = true;

    /**
     * Base URL for BCCR (Banco Central de Costa Rica) SINPE API.
     * Production: https://www.bccr.fi.cr/sinpe
     * Sandbox: https://sandbox.bccr.fi.cr/sinpe
     */
    @NotBlank
    private String baseUrl = "https://sandbox.bccr.fi.cr/sinpe";

    /**
     * Sender's registered phone number for SINPE Móvil.
     * Format: 8 digits (e.g., 88888888)
     */
    @NotBlank
    @Size(min = 8, max = 8, message = "SINPE source phone must be exactly 8 digits")
    private String sourcePhone;

    /**
     * Sender's Costa Rican IBAN for SINPE transfers.
     * Format: CR + 20 digits (e.g., CR02010200009088888888)
     */
    @NotBlank
    @Size(min = 22, max = 22, message = "Costa Rican IBAN must be exactly 22 characters (CR + 20 digits)")
    private String sourceIban;

    /**
     * Financial institution code assigned by BCCR.
     */
    @NotBlank
    @Size(max = 10)
    private String institutionCode;

    /**
     * Webhook URL for SINPE status notifications.
     * Must be HTTPS and registered with BCCR.
     */
    @NotBlank
    private String webhookUrl;

    /**
     * Secret for validating SINPE webhook signatures (HMAC).
     */
    @NotBlank
    private String webhookSecret;

    /**
     * API timeout for SINPE operations.
     */
    @NotNull
    private Duration timeout = Duration.ofSeconds(30);

    /**
     * Maximum amount allowed per SINPE Móvil transaction (CRC).
     * BCCR limit: CRC 100,000 (~USD 200)
     */
    @NotNull
    @Positive
    private BigDecimal mobileMaxAmount = new BigDecimal("100000.00");

    /**
     * Maximum amount allowed per SINPE IBAN transaction (CRC).
     * Higher limit than mobile.
     */
    @NotNull
    @Positive
    private BigDecimal ibanMaxAmount = new BigDecimal("10000000.00"); // CRC 10M

    /**
     * Whether to send SMS notifications to recipients.
     */
    @NotNull
    private Boolean smsNotificationEnabled = true;

    /**
     * Default transfer channel: MOBILE (SINPE Móvil) or IBAN (SINPE).
     */
    @NotBlank
    @Size(max = 10)
    private String defaultChannel = "MOBILE";

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getSourcePhone() {
        return sourcePhone;
    }

    public void setSourcePhone(String sourcePhone) {
        this.sourcePhone = sourcePhone;
    }

    public String getSourceIban() {
        return sourceIban;
    }

    public void setSourceIban(String sourceIban) {
        this.sourceIban = sourceIban;
    }

    public String getInstitutionCode() {
        return institutionCode;
    }

    public void setInstitutionCode(String institutionCode) {
        this.institutionCode = institutionCode;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public BigDecimal getMobileMaxAmount() {
        return mobileMaxAmount;
    }

    public void setMobileMaxAmount(BigDecimal mobileMaxAmount) {
        this.mobileMaxAmount = mobileMaxAmount;
    }

    public BigDecimal getIbanMaxAmount() {
        return ibanMaxAmount;
    }

    public void setIbanMaxAmount(BigDecimal ibanMaxAmount) {
        this.ibanMaxAmount = ibanMaxAmount;
    }

    public Boolean getSmsNotificationEnabled() {
        return smsNotificationEnabled;
    }

    public void setSmsNotificationEnabled(Boolean smsNotificationEnabled) {
        this.smsNotificationEnabled = smsNotificationEnabled;
    }

    public String getDefaultChannel() {
        return defaultChannel;
    }

    public void setDefaultChannel(String defaultChannel) {
        this.defaultChannel = defaultChannel;
    }

    // -------------------------------------------------------------------------
    // Business Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the full webhook URL with path.
     */
    public String getFullWebhookUrl() {
        return webhookUrl + "/webhooks/sinpe";
    }

    /**
     * Returns the appropriate max amount based on channel.
     */
    public BigDecimal resolveMaxAmount(String channel) {
        if ("IBAN".equalsIgnoreCase(channel)) {
            return ibanMaxAmount;
        }
        return mobileMaxAmount;
    }

    /**
     * Checks if the given amount is within limits for the specified channel.
     */
    public boolean isWithinLimit(BigDecimal amount, String channel) {
        BigDecimal max = resolveMaxAmount(channel);
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0
            && amount.compareTo(max) <= 0;
    }
}