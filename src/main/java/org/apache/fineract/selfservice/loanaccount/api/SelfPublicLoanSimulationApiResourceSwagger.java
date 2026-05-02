/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.selfservice.loanaccount.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

/** OpenAPI schema definitions for the public loan simulation endpoints (MX-250). */
@SuppressWarnings({"MemberName"})
final class SelfPublicLoanSimulationApiResourceSwagger {

  private SelfPublicLoanSimulationApiResourceSwagger() {}

  @Schema(description = "GetSelfPublicLoanSimulationProductsResponse")
  public static final class GetSelfPublicSimulationProductsResponse {

    private GetSelfPublicSimulationProductsResponse() {}

    @Schema(example = "1")
    public Long id;

    @Schema(example = "Personal Loan")
    public String name;

    @Schema(example = "PL")
    public String shortName;

    @Schema(example = "A standard personal loan product")
    public String description;

    @Schema(example = "USD")
    public String currencyCode;

    @Schema(example = "10000.000000")
    public BigDecimal principal;

    @Schema(example = "1000.000000")
    public BigDecimal minPrincipal;

    @Schema(example = "100000.000000")
    public BigDecimal maxPrincipal;

    @Schema(example = "12.000000")
    public BigDecimal interestRatePerPeriod;

    @Schema(example = "5.000000")
    public BigDecimal minInterestRatePerPeriod;

    @Schema(example = "25.000000")
    public BigDecimal maxInterestRatePerPeriod;

    @Schema(example = "12")
    public Integer numberOfRepayments;

    @Schema(example = "1")
    public Integer minNumberOfRepayments;

    @Schema(example = "36")
    public Integer maxNumberOfRepayments;

    @Schema(example = "1")
    public Integer repaymentEvery;

    @Schema(example = "mifos-standard-strategy")
    public String transactionProcessingStrategyCode;
  }

  @Schema(
      description = "PostSelfPublicLoanSimulationRequest",
      requiredProperties = {
        "productId",
        "principal",
        "loanTermFrequency",
        "loanTermFrequencyType",
        "numberOfRepayments",
        "repaymentEvery",
        "repaymentFrequencyType",
        "interestRatePerPeriod",
        "amortizationType",
        "interestType",
        "interestCalculationPeriodType",
        "expectedDisbursementDate",
        "transactionProcessingStrategyCode",
        "dateFormat",
        "locale"
      })
  public static final class PostSelfPublicSimulationRequest {

    private PostSelfPublicSimulationRequest() {}

    @Schema(example = "1")
    public Long productId;

    @Schema(example = "10000")
    public BigDecimal principal;

    @Schema(example = "12")
    public Integer loanTermFrequency;

    @Schema(example = "2", description = "0=Days, 1=Weeks, 2=Months, 3=Years")
    public Integer loanTermFrequencyType;

    @Schema(example = "12")
    public Integer numberOfRepayments;

    @Schema(example = "1")
    public Integer repaymentEvery;

    @Schema(example = "2", description = "0=Days, 1=Weeks, 2=Months, 3=Years")
    public Integer repaymentFrequencyType;

    @Schema(example = "12")
    public BigDecimal interestRatePerPeriod;

    @Schema(example = "1", description = "0=Equal Principal Payments, 1=Equal Installments")
    public Integer amortizationType;

    @Schema(example = "0", description = "0=Declining Balance, 1=Flat")
    public Integer interestType;

    @Schema(
        example = "1",
        description = "0=Daily, 1=Same as repayment period, 2=Daily (Compounding)")
    public Integer interestCalculationPeriodType;

    @Schema(example = "01 June 2026")
    public String expectedDisbursementDate;

    @Schema(example = "mifos-standard-strategy")
    public String transactionProcessingStrategyCode;

    @Schema(example = "dd MMMM yyyy")
    public String dateFormat;

    @Schema(example = "en")
    public String locale;
  }

  @Schema(description = "PostSelfPublicLoanSimulationResponse")
  public static final class PostSelfPublicSimulationResponse {

    private PostSelfPublicSimulationResponse() {}

    static final class Currency {

      private Currency() {}

      @Schema(example = "USD")
      public String code;

      @Schema(example = "US Dollar")
      public String name;

      @Schema(example = "2")
      public Integer decimalPlaces;

      @Schema(example = "$")
      public String displaySymbol;
    }

    @Schema(description = "Currency details")
    public Currency currency;

    @Schema(example = "10")
    public Integer loanTermInDays;

    @Schema(example = "10000.000000")
    public BigDecimal totalPrincipalDisbursed;

    @Schema(example = "10000.000000")
    public BigDecimal totalPrincipalExpected;

    @Schema(example = "600.000000")
    public BigDecimal totalInterestCharged;

    @Schema(example = "0.000000")
    public BigDecimal totalFeeChargesCharged;

    @Schema(example = "0.000000")
    public BigDecimal totalPenaltyChargesCharged;

    @Schema(example = "10600.000000")
    public BigDecimal totalRepaymentExpected;

    @Schema(description = "Repayment schedule periods")
    public java.util.List<SchedulePeriod> periods;

    static final class SchedulePeriod {

      private SchedulePeriod() {}

      @Schema(example = "1")
      public Integer period;

      @Schema(example = "[2026, 7, 1]")
      public LocalDate dueDate;

      @Schema(example = "833.33")
      public BigDecimal principalDue;

      @Schema(example = "100.00")
      public BigDecimal interestDue;

      @Schema(example = "933.33")
      public BigDecimal totalDueForPeriod;

      @Schema(example = "9166.67")
      public BigDecimal totalOutstandingForPeriod;
    }
  }
}
