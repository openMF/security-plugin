/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.providers.base;

import community.mifos.payments.infrastructure.exception.ProviderNotFoundException;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for obtaining the correct payment provider based on country/payment type.
 * Implements Registry pattern for provider discovery.
 */
@Component
public class PaymentProviderFactory {
    
    private final Map<String, PaymentProvider> providersByCode;
    private final Map<String, PaymentProvider> providersByCountry;
    
    public PaymentProviderFactory(List<PaymentProvider> providers) {
        this.providersByCode = providers.stream()
            .collect(Collectors.toMap(PaymentProvider::getProviderCode, Function.identity()));
        this.providersByCountry = providers.stream()
            .collect(Collectors.toMap(PaymentProvider::getCountryCode, Function.identity()));
    }
    
    public PaymentProvider getProvider(String providerCode) {
        return Optional.ofNullable(providersByCode.get(providerCode.toUpperCase()))
            .orElseThrow(() -> new ProviderNotFoundException("Provider not found: " + providerCode));
    }
    
    public PaymentProvider getProviderByCountry(String countryCode) {
        return Optional.ofNullable(providersByCountry.get(countryCode.toUpperCase()))
            .orElseThrow(() -> new ProviderNotFoundException("No provider available for country: " + countryCode));
    }
    
    public List<PaymentProvider> getAllProviders() {
        return List.copyOf(providersByCode.values());
    }
    
    public boolean isSupported(String countryCode) {
        return providersByCountry.containsKey(countryCode.toUpperCase());
    }
}