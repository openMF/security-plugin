/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.webhook;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Handles incoming webhooks from payment providers
 */
public interface WebhookHandler {
    String getProviderCode();
    void handleWebhook(JsonNode payload, String signature);
}

