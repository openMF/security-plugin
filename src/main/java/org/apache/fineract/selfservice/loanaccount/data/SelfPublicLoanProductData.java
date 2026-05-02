/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.selfservice.loanaccount.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

/**
 * Reduced loan product data for public simulation endpoints. Contains only the fields needed to
 * populate a loan simulation UI — product identity, currency, principal ranges, interest rates,
 * repayment configuration, and strategy.
 *
 * <p>This is intentionally separate from the core {@code LoanProductData} which has 100+ fields and
 * a deeply coupled constructor. Using a separate DTO avoids importing charges, rates,
 * borrower-cycle-variations, guarantees, and other data that requires authenticated access to
 * retrieve.
 */
@Getter
@Builder
public class SelfPublicLoanProductData {

  private final Long id;
  private final String name;
  private final String shortName;
  private final String description;

  // Currency
  private final String currencyCode;
  private final String currencyName;
  private final String currencyDisplaySymbol;
  private final Integer currencyDigits;
  private final Integer currencyInMultiplesOf;

  // Principal
  private final BigDecimal principal;
  private final BigDecimal minPrincipal;
  private final BigDecimal maxPrincipal;

  // Interest
  private final BigDecimal interestRatePerPeriod;
  private final BigDecimal minInterestRatePerPeriod;
  private final BigDecimal maxInterestRatePerPeriod;
  private final BigDecimal annualInterestRate;
  private final Integer interestRatePerPeriodFrequencyType;
  private final Integer interestType;
  private final Integer interestCalculationPeriodType;

  // Repayment
  private final Integer numberOfRepayments;
  private final Integer minNumberOfRepayments;
  private final Integer maxNumberOfRepayments;
  private final Integer repaymentEvery;
  private final Integer repaymentFrequencyType;

  // Amortization
  private final Integer amortizationType;

  // Strategy
  private final String transactionProcessingStrategyCode;
  private final String transactionProcessingStrategyName;

  // Configuration
  private final Integer daysInMonthType;
  private final Integer daysInYearType;
  private final Boolean multiDisburseLoan;
  private final Integer accountingType;
  private final BigDecimal inArrearsTolerance;

  // Dates
  private final LocalDate startDate;
  private final LocalDate closeDate;
}
