/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.selfservice.account.api;

import community.mifos.payments.core.domain.FastPayment;
import community.mifos.payments.core.domain.PaymentTransaction;
import community.mifos.payments.core.service.FastPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

/**
 * Self-Service API for Fast Payments (PIX, SPEI, SINPE)
 * Exposed to mobile/web self-service clients
 */
@Path("/v1/self/payments")
@Component
@Tag(name = "Fast Payments", description = "Instant payment transfers for LATAM")
@RequiredArgsConstructor
public class FastPaymentApiResource {
    
    private final FastPaymentService paymentService;
    
    @POST
    @Path("/transfer")
    @PreAuthorize("hasRole('SELF_SERVICE_USER')")
    @Operation(summary = "Initiate fast payment transfer",
               description = "Supports PIX (BR), SPEI (MX), SINPE (CR)")
    public ResponseEntity<PaymentTransaction> initiateTransfer(
            @Valid @RequestBody FastPaymentRequest request,
            @Parameter(hidden = true) @RequestHeader("X-Client-Geo-Country") String countryCode) {
        
        FastPayment payment = FastPayment.builder()
            .amount(request.amount())
            .currency(request.currency())
            .recipientIdentifier(request.recipientIdentifier())
            .recipientName(request.recipientName())
            .description(request.description())
            .sourceAccountId(request.sourceAccountId())
            .countryCode(countryCode != null ? countryCode : request.countryCode())
            .build();
        
        PaymentTransaction transaction = paymentService.processPayment(payment);
        return ResponseEntity.ok(transaction);
    }
    
    @GetMapping("/transfer/{transactionId}/status")
    @PreAuthorize("hasRole('SELF_SERVICE_USER')")
    @Operation(summary = "Query transaction status")
    public ResponseEntity<PaymentTransaction> getTransactionStatus(
            @PathVariable String transactionId,
            @RequestParam String provider) {
        
        PaymentTransaction transaction = paymentService.queryStatus(transactionId, provider);
        return ResponseEntity.ok(transaction);
    }
    
    @PostMapping("/validate-recipient")
    @PreAuthorize("hasRole('SELF_SERVICE_USER')")
    @Operation(summary = "Validate recipient before transfer")
    public ResponseEntity<ValidationResponse> validateRecipient(
            @RequestParam String identifier,
            @RequestParam String countryCode,
            @RequestParam(required = false) String providerCode) {
        
        boolean isValid = paymentService.validateRecipient(identifier, countryCode, providerCode);
        String recipientName = isValid ? 
            paymentService.lookupRecipientName(identifier, countryCode, providerCode) : null;
        
        return ResponseEntity.ok(new ValidationResponse(isValid, recipientName));
    }
    
    @PostMapping("/qr-code")
    @PreAuthorize("hasRole('SELF_SERVICE_USER')")
    @Operation(summary = "Generate QR code for receiving payment")
    public ResponseEntity<QrCodeResponse> generateQrCode(
            @RequestParam BigDecimal amount,
            @RequestParam String description,
            @RequestParam String countryCode,
            @RequestParam(required = false) String providerCode) {
        
        String qrData = paymentService.generatePaymentToken(amount, description, countryCode, providerCode);
        return ResponseEntity.ok(new QrCodeResponse(qrData));
    }
    
    @DeleteMapping("/transfer/{transactionId}")
    @PreAuthorize("hasRole('SELF_SERVICE_USER')")
    @Operation(summary = "Cancel pending transaction")
    public ResponseEntity<Void> cancelTransaction(
            @PathVariable String transactionId,
            @RequestParam String provider) {
        
        boolean cancelled = paymentService.cancelTransaction(transactionId, provider);
        return cancelled ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }
}

// DTOs
record FastPaymentRequest(
    java.math.BigDecimal amount,
    String currency,
    String recipientIdentifier,
    String recipientName,
    String description,
    Long sourceAccountId,
    String countryCode
) {}

record ValidationResponse(boolean valid, String recipientName) {}
record QrCodeResponse(String qrCodeData) {}