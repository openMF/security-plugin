/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.infrastructure.security;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Validates webhook signatures from payment providers.
 * 
 * PIX (Brazil): JWS with RS256 (RSA + SHA-256) using BACEN public key.
 * SPEI (Mexico): HMAC-SHA256 with shared secret.
 * SINPE (Costa Rica): HMAC-SHA256 with shared secret.
 */
@Component
public class WebhookSignatureValidator {

    private static final Logger LOG = LoggerFactory.getLogger(WebhookSignatureValidator.class);

    private final String pixWebhookPublicKeyPem;
    private final String speiWebhookSecret;
    private final String sinpeWebhookSecret;

    public WebhookSignatureValidator(
            @Value("${payments.security.pix-webhook-public-key:}") String pixWebhookPublicKeyPem,
            @Value("${payments.security.spei-webhook-secret:}") String speiWebhookSecret,
            @Value("${payments.security.sinpe-webhook-secret:}") String sinpeWebhookSecret) {
        this.pixWebhookPublicKeyPem = pixWebhookPublicKeyPem;
        this.speiWebhookSecret = speiWebhookSecret;
        this.sinpeWebhookSecret = sinpeWebhookSecret;
    }

    /**
     * Validates PIX webhook JWS signature from BACEN.
     * 
     * @param payload   the parsed JSON body
     * @param signature the JWS compact serialization from the webhook header
     * @return true if signature is valid
     */
    public boolean validatePixWebhook(JsonNode payload, String signature) {
        if (signature == null || signature.isBlank()) {
            LOG.warn("Missing PIX webhook signature");
            return false;
        }

        try {
            // JWS compact serialization: base64url(header).base64url(payload).base64url(signature)
            String[] parts = signature.split("\\.");
            if (parts.length != 3) {
                LOG.warn("Invalid PIX JWS format, expected 3 parts but got {}", parts.length);
                return false;
            }

            byte[] payloadBytes = extractPayloadBytes(parts[1], payload);
            byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[2]);

            PublicKey publicKey = loadRsaPublicKey(pixWebhookPublicKeyPem);
            if (publicKey == null) {
                LOG.error("PIX webhook public key is not configured");
                return false;
            }

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(payloadBytes);

            boolean valid = sig.verify(signatureBytes);
            if (!valid) {
                LOG.warn("PIX webhook signature verification failed for txid: {}", 
                    payload.has("txid") ? payload.get("txid").asText() : "unknown");
            }
            return valid;

        } catch (Exception e) {
            LOG.error("PIX webhook signature validation failed", e);
            return false;
        }
    }

    /**
     * Validates SPEI webhook HMAC signature.
     */
    public boolean validateSpeiWebhook(JsonNode payload, String signature) {
        if (signature == null || signature.isBlank() || 
            speiWebhookSecret == null || speiWebhookSecret.isBlank()) {
            LOG.warn("Missing SPEI webhook signature or secret");
            return false;
        }

        try {
            String expected = calculateHmacSha256(payload.toString(), speiWebhookSecret);
            return signature.equals(expected);
        } catch (Exception e) {
            LOG.error("SPEI webhook signature validation failed", e);
            return false;
        }
    }

    /**
     * Validates SINPE webhook HMAC signature.
     */
    public boolean validateSinpeWebhook(JsonNode payload, String signature) {
        if (signature == null || signature.isBlank() || 
            sinpeWebhookSecret == null || sinpeWebhookSecret.isBlank()) {
            LOG.warn("Missing SINPE webhook signature or secret");
            return false;
        }

        try {
            String expected = calculateHmacSha256(payload.toString(), sinpeWebhookSecret);
            return signature.equals(expected);
        } catch (Exception e) {
            LOG.error("SINPE webhook signature validation failed", e);
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Private Helpers
    // -------------------------------------------------------------------------

    /**
     * Extracts payload bytes from JWS or falls back to request body.
     * Handles both embedded and detached JWS payloads.
     */
    private byte[] extractPayloadBytes(String encodedPayload, JsonNode payload) {
        if (encodedPayload == null || encodedPayload.isEmpty()) {
            // Detached payload: use the HTTP request body
            return payload.toString().getBytes(StandardCharsets.UTF_8);
        }

        try {
            byte[] decoded = Base64.getUrlDecoder().decode(encodedPayload);
            String decodedStr = new String(decoded, StandardCharsets.UTF_8);
            String bodyStr = payload.toString();

            if (!decodedStr.equals(bodyStr)) {
                LOG.warn("PIX webhook JWS payload does not match request body");
            }
            return decoded;
        } catch (IllegalArgumentException e) {
            LOG.warn("Failed to decode JWS payload, falling back to request body");
            return payload.toString().getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * Loads RSA public key from PEM format.
     */
    private PublicKey loadRsaPublicKey(String pem) throws Exception {
        if (pem == null || pem.isBlank()) {
            return null;
        }

        String cleanPem = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                             .replace("-----END PUBLIC KEY-----", "")
                             .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(cleanPem);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    /**
     * Calculates HMAC-SHA256 and returns Base64-encoded result.
     */
    private String calculateHmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}