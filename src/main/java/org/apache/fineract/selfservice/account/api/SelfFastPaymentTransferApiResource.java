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
package org.apache.fineract.selfservice.account.api;

import community.mifos.payments.api.FastPaymentRequest;
import community.mifos.payments.core.domain.FastPayment;
import community.mifos.payments.core.domain.PaymentTransaction;
import community.mifos.payments.core.service.FastPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestHeader;

@Path("/v1/self/payments")
@Component
@Tag(name = "Self Fast Payment transfer", description = "")
@RequiredArgsConstructor
public class SelfFastPaymentTransferApiResource {

    private final FastPaymentService paymentService;

    @POST
    @Path("/transfer")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
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
    
}
