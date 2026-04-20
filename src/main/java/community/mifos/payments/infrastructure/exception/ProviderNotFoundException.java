/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.infrastructure.exception;

/**
 * Exception thrown when a requested payment provider is not found,
 * not configured, or disabled for the specified country.
 */
public class ProviderNotFoundException extends PaymentException {

    private static final long serialVersionUID = 1L;

    /**
     * Country code that was requested (BR, MX, CR)
     */
    private final String countryCode;

    /**
     * Creates exception for missing provider configuration.
     */
    public ProviderNotFoundException(String providerCode) {
        super("Payment provider not found or disabled: " + providerCode, 
              "PROVIDER_NOT_FOUND");
        this.countryCode = null;
    }

    /**
     * Creates exception with country context for auto-resolution failures.
     */
    public ProviderNotFoundException(String providerCode, String countryCode) {
        super("Payment provider not available for country " + countryCode + 
              ": " + providerCode, "PROVIDER_NOT_FOUND");
        this.countryCode = countryCode;
    }

    /**
     * Creates exception with cause (e.g., configuration load failure).
     */
    public ProviderNotFoundException(String providerCode, String countryCode, Throwable cause) {
        super("Payment provider not found or disabled: " + providerCode, 
              "PROVIDER_NOT_FOUND", cause);
        this.countryCode = countryCode;
    }

    public String getCountryCode() {
        return countryCode;
    }
}