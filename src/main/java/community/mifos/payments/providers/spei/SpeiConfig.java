package community.mifos.payments.providers.spei;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Configuration properties for Mexico's SPEI (Sistema de Pagos Electrónicos Interbancarios).
 * Covers SPEI transfers, CoDi (Cobro Digital), and scheduled payments.
 * Loaded from application.yml or environment variables.
 */
@ConfigurationProperties(prefix = "payments.providers.spei")
@Validated
public class SpeiConfig {

    /**
     * Whether SPEI integration is enabled.
     */
    @NotNull
    private Boolean enabled = true;

    /**
     * Base URL for Banxico (Banco de México) SPEI API.
     * Production: https://www.banxico.org.mx/spei
     * Sandbox: https://sandbox.banxico.org.mx/spei
     */
    @NotBlank
    private String baseUrl = "https://sandbox.banxico.org.mx/spei";

    /**
     * OAuth2 client ID registered with Banxico.
     */
    @NotBlank
    private String clientId;

    /**
     * OAuth2 client secret registered with Banxico.
     */
    @NotBlank
    private String clientSecret;

    /**
     * Path to the SPEI certificate (PFX or PEM) for mutual TLS.
     */
    private String certificatePath;

    /**
     * Password for the SPEI certificate.
     */
    private String certificatePassword;

    /**
     * Sender's CLABE (18-digit standardized bank account number).
     */
    @NotBlank
    @Size(min = 18, max = 18, message = "CLABE must be exactly 18 digits")
    private String sourceClabe;

    /**
     * Sender's institution code (3 digits, e.g., 401 for Banorte).
     */
    @NotBlank
    @Size(min = 3, max = 3, message = "Institution code must be exactly 3 digits")
    private String institutionCode;

    /**
     * Sender's RFC (Tax ID) for regulatory reporting.
     */
    @NotBlank
    @Size(min = 12, max = 13, message = "RFC must be 12 (moral) or 13 (física) characters")
    private String sourceRfc;

    /**
     * Webhook URL for SPEI/CoDi status notifications.
     * Must be HTTPS and registered with Banxico.
     */
    @NotBlank
    private String webhookUrl;

    /**
     * Secret for validating SPEI webhook signatures (HMAC or JWS).
     */
    @NotBlank
    private String webhookSecret;

    /**
     * API timeout for SPEI operations.
     */
    @NotNull
    private Duration timeout = Duration.ofSeconds(30);

    /**
     * Maximum amount allowed per SPEI same-day transaction (MXN).
     * Banxico limit: MXN 800,000 for immediate transfers.
     */
    @NotNull
    @Positive
    private BigDecimal sameDayMaxAmount = new BigDecimal("800000.00");

    /**
     * Maximum amount allowed per CoDi transaction (MXN).
     * CoDi limit: MXN 30,000 per transaction.
     */
    @NotNull
    @Positive
    private BigDecimal codiMaxAmount = new BigDecimal("30000.00");

    /**
     * Whether CoDi (Cobro Digital) QR payments are enabled.
     */
    @NotNull
    private Boolean codiEnabled = true;

    /**
     * CoDi webhook URL (can be separate from SPEI webhooks).
     */
    private String codiWebhookUrl;

    /**
     * Default settlement type: SAME_DAY (SPEI), NEXT_DAY, or SCHEDULED.
     */
    @NotBlank
    @Size(max = 20)
    private String defaultSettlementType = "SAME_DAY";

    /**
     * SPEI operating hours check (transfers outside hours go to next business day).
     */
    @NotNull
    private Boolean enforceOperatingHours = true;

    /**
     * SPEI operating hours: start time (HH:mm).
     */
    private String operatingHoursStart = "06:00";

    /**
     * SPEI operating hours: end time (HH:mm).
     */
    private String operatingHoursEnd = "17:30";

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
        return clientId;
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

    public String getSourceClabe() {
        return sourceClabe;
    }

    public void setSourceClabe(String sourceClabe) {
        this.sourceClabe = sourceClabe;
    }

    public String getInstitutionCode() {
        return institutionCode;
    }

    public void setInstitutionCode(String institutionCode) {
        this.institutionCode = institutionCode;
    }

    public String getSourceRfc() {
        return sourceRfc;
    }

    public void setSourceRfc(String sourceRfc) {
        this.sourceRfc = sourceRfc;
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

    public BigDecimal getSameDayMaxAmount() {
        return sameDayMaxAmount;
    }

    public void setSameDayMaxAmount(BigDecimal sameDayMaxAmount) {
        this.sameDayMaxAmount = sameDayMaxAmount;
    }

    public BigDecimal getCodiMaxAmount() {
        return codiMaxAmount;
    }

    public void setCodiMaxAmount(BigDecimal codiMaxAmount) {
        this.codiMaxAmount = codiMaxAmount;
    }

    public Boolean getCodiEnabled() {
        return codiEnabled;
    }

    public void setCodiEnabled(Boolean codiEnabled) {
        this.codiEnabled = codiEnabled;
    }

    public String getCodiWebhookUrl() {
        return codiWebhookUrl;
    }

    public void setCodiWebhookUrl(String codiWebhookUrl) {
        this.codiWebhookUrl = codiWebhookUrl;
    }

    public String getDefaultSettlementType() {
        return defaultSettlementType;
    }

    public void setDefaultSettlementType(String defaultSettlementType) {
        this.defaultSettlementType = defaultSettlementType;
    }

    public Boolean getEnforceOperatingHours() {
        return enforceOperatingHours;
    }

    public void setEnforceOperatingHours(Boolean enforceOperatingHours) {
        this.enforceOperatingHours = enforceOperatingHours;
    }

    public String getOperatingHoursStart() {
        return operatingHoursStart;
    }

    public void setOperatingHoursStart(String operatingHoursStart) {
        this.operatingHoursStart = operatingHoursStart;
    }

    public String getOperatingHoursEnd() {
        return operatingHoursEnd;
    }

    public void setOperatingHoursEnd(String operatingHoursEnd) {
        this.operatingHoursEnd = operatingHoursEnd;
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
        return webhookUrl + "/webhooks/spei";
    }

    /**
     * Returns the CoDi-specific webhook URL.
     */
    public String getFullCodiWebhookUrl() {
        return (codiWebhookUrl != null ? codiWebhookUrl : webhookUrl) + "/webhooks/codi";
    }

    /**
     * Returns the appropriate max amount based on transfer type.
     */
    public BigDecimal resolveMaxAmount(boolean isCoDi) {
        if (isCoDi) {
            return codiMaxAmount;
        }
        return sameDayMaxAmount;
    }

    /**
     * Checks if the given amount is within limits for the specified transfer type.
     */
    public boolean isWithinLimit(BigDecimal amount, boolean isCoDi) {
        BigDecimal max = resolveMaxAmount(isCoDi);
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0
            && amount.compareTo(max) <= 0;
    }

    /**
     * Checks if current time is within SPEI operating hours.
     * SPEI operates 06:00-17:30 Mexico City time (CST/CDT).
     */
    public boolean isWithinOperatingHours() {
        if (!Boolean.TRUE.equals(enforceOperatingHours)) {
            return true;
        }

        java.time.LocalTime now = java.time.LocalTime.now(
            java.time.ZoneId.of("America/Mexico_City")
        );

        java.time.LocalTime start = java.time.LocalTime.parse(operatingHoursStart);
        java.time.LocalTime end = java.time.LocalTime.parse(operatingHoursEnd);

        return !now.isBefore(start) && !now.isAfter(end);
    }

    /**
     * Returns the next business day for scheduled transfers outside operating hours.
     */
    public LocalDateTime getNextSettlementDate() {
        java.time.ZoneId mexicoCity = java.time.ZoneId.of("America/Mexico_City");
        java.time.LocalDateTime now = java.time.LocalDateTime.now(mexicoCity);
        java.time.LocalTime start = java.time.LocalTime.parse(operatingHoursStart);

        java.time.LocalDate nextBusinessDay = now.toLocalDate();
        if (now.toLocalTime().isAfter(start)) {
            nextBusinessDay = nextBusinessDay.plusDays(1);
        }

        // Skip weekends (simple check; production should use Mexican holiday calendar)
        while (nextBusinessDay.getDayOfWeek() == java.time.DayOfWeek.SATURDAY
            || nextBusinessDay.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            nextBusinessDay = nextBusinessDay.plusDays(1);
        }

        return nextBusinessDay.atTime(start).atZone(mexicoCity).toLocalDateTime();
    }
}