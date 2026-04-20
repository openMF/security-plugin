/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.providers.pix;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Configuration properties for Brazil's PIX (Payment Instantaneous System).
 * Loaded from application.yml or environment variables.
 */
@ConfigurationProperties(prefix = "payments.providers.pix")
@Validated
public class PixConfig {

    /**
     * Whether PIX integration is enabled.
     */
    @NotNull
    private Boolean enabled = true;

    /**
     * Base URL for BACEN (Central Bank of Brazil) PIX API.
     * Production: https://api.bcb.gov.br/pix
     * Sandbox: https://api-h.bcb.gov.br/pix
     */
    @NotBlank
    private String baseUrl = "https://api-h.bcb.gov.br/pix";

    /**
     * OAuth2 client ID registered with BACEN.
     */
    @NotBlank
    private String clientId;

    /**
     * OAuth2 client secret registered with BACEN.
     */
    @NotBlank
    private String clientSecret;

    /**
     * Path to the PIX certificate (PFX or PEM) for mutual TLS.
     */
    private String certificatePath;

    /**
     * Password for the PIX certificate.
     */
    private String certificatePassword;

    /**
     * PIX key (EVP) registered for this institution to receive payments.
     * Format: UUID v4 (e.g., 123e4567-e89b-12d3-a456-426614174000)
     */
    private String pixKey;

    /**
     * Webhook URL for PIX status notifications.
     * Must be HTTPS and registered with BACEN.
     */
    @NotBlank
    private String webhookUrl;

    /**
     * Secret for validating PIX webhook signatures (JWS).
     */
    @NotBlank
    private String webhookSecret;

    /**
     * API timeout for PIX operations.
     */
    @NotNull
    private Duration timeout = Duration.ofSeconds(30);

    /**
     * Maximum amount allowed per PIX transaction (BRL).
     * BACEN limit: R$ 1,000,000.00
     */
    @NotNull
    @Positive
    private BigDecimal maxAmount = new BigDecimal("1000000.00");

    /**
     * Whether to use PIX Cobrança (charge) or Pix Venda (payment).
     * Cobrança = true (generates QR code, 24h expiry)
     * Venda = false (immediate transfer)
     */
    @NotNull
    private Boolean cobrancaMode = true;

    /**
     * Default expiry hours for PIX Cobrança.
     */
    @NotNull
    @Positive
    private Integer defaultExpiryHours = 24;

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

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getCertificatePath() {
        return certificatePath;
    }

    public void setCertificatePath(String certificatePath) {
        this.certificatePath = certificatePath;
    }

    public String getCertificatePassword() {
        return certificatePassword;
    }

    public void setCertificatePassword(String certificatePassword) {
        this.certificatePassword = certificatePassword;
    }

    public String getPixKey() {
        return pixKey;
    }

    public void setPixKey(String pixKey) {
        this.pixKey = pixKey;
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

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public Boolean getCobrancaMode() {
        return cobrancaMode;
    }

    public void setCobrancaMode(Boolean cobrancaMode) {
        this.cobrancaMode = cobrancaMode;
    }

    public Integer getDefaultExpiryHours() {
        return defaultExpiryHours;
    }

    public void setDefaultExpiryHours(Integer defaultExpiryHours) {
        this.defaultExpiryHours = defaultExpiryHours;
    }

    // -------------------------------------------------------------------------
    // Business Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns true if certificate-based mutual TLS is configured.
     */
    public boolean hasCertificateAuthentication() {
        return certificatePath != null && !certificatePath.isBlank();
    }

    /**
     * Returns the full webhook URL with path.
     */
    public String getFullWebhookUrl() {
        return webhookUrl + "/webhooks/pix";
    }

    /**
     * Calculates expiry date based on Cobrança mode.
     */
    public java.time.LocalDateTime calculateExpiry(java.time.LocalDateTime createdAt) {
        if (Boolean.TRUE.equals(cobrancaMode)) {
            return createdAt.plusHours(defaultExpiryHours);
        }
        // Pix Venda expires in 5 minutes for QR code generation
        return createdAt.plusMinutes(5);
    }
}