/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.providers.pix;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Validation service for PIX (Brazil) payment identifiers and parameters.
 * Implements BACEN (Banco Central do Brasil) validation rules.
 */
@Service
public class PixValidationService {

    // -------------------------------------------------------------------------
    // PIX Key Patterns (Chaves PIX)
    // -------------------------------------------------------------------------

    /**
     * CPF: 11 digits, all numeric
     */
    private static final Pattern CPF_PATTERN = Pattern.compile("^\\d{11}$");

    /**
     * CNPJ: 14 digits, all numeric
     */
    private static final Pattern CNPJ_PATTERN = Pattern.compile("^\\d{14}$");

    /**
     * Phone: +5511... or 5511... (Brazilian mobile)
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?55\\d{2}\\d{8,9}$");

    /**
     * Email: standard email format
     */
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * EVP (Endereço Virtual de Pagamento): UUID v4 format
     */
    private static final Pattern EVP_PATTERN = 
        Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$");

    // -------------------------------------------------------------------------
    // Validation Methods
    // -------------------------------------------------------------------------

    /**
     * Determines the type of PIX key and validates format.
     * 
     * @param pixKey the PIX key to validate
     * @return true if format is valid (syntax only)
     */
    public boolean isValidPixKey(String pixKey) {
        if (pixKey == null || pixKey.isBlank()) {
            return false;
        }

        String normalized = normalizePixKey(pixKey);

        return switch (detectPixKeyType(normalized)) {
            case CPF -> validateCpf(normalized);
            case CNPJ -> validateCnpj(normalized);
            case PHONE -> validatePhone(normalized);
            case EMAIL -> validateEmail(normalized);
            case EVP -> validateEvp(normalized);
            default -> false;
        };
    }

    /**
     * Validates CPF format and check digits.
     */
    public boolean validateCpf(String cpf) {
        if (!CPF_PATTERN.matcher(cpf).matches()) {
            return false;
        }

        // Validate check digits
        int[] digits = cpf.chars().map(c -> c - '0').toArray();
        
        // First check digit
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += digits[i] * (10 - i);
        }
        int firstCheck = 11 - (sum % 11);
        if (firstCheck > 9) firstCheck = 0;
        if (digits[9] != firstCheck) return false;

        // Second check digit
        sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += digits[i] * (11 - i);
        }
        int secondCheck = 11 - (sum % 11);
        if (secondCheck > 9) secondCheck = 0;
        
        return digits[10] == secondCheck;
    }

    /**
     * Validates CNPJ format and check digits.
     */
    public boolean validateCnpj(String cnpj) {
        if (!CNPJ_PATTERN.matcher(cnpj).matches()) {
            return false;
        }

        int[] digits = cnpj.chars().map(c -> c - '0').toArray();
        int[] weights = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

        // First check digit
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += digits[i] * weights[i];
        }
        int firstCheck = sum % 11;
        if (firstCheck < 2) firstCheck = 0;
        if (digits[12] != firstCheck) return false;

        // Second check digit
        sum = 0;
        for (int i = 0; i < 13; i++) {
            sum += digits[i] * weights[i];
        }
        int secondCheck = sum % 11;
        if (secondCheck < 2) secondCheck = 0;

        return digits[13] == secondCheck;
    }

    /**
     * Validates Brazilian phone number format.
     */
    public boolean validatePhone(String phone) {
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            return false;
        }
        // Remove +55 prefix if present for length check
        String digits = phone.replaceAll("\\D", "");
        return digits.length() >= 10 && digits.length() <= 11; // With or without 9th digit
    }

    /**
     * Validates email format.
     */
    public boolean validateEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validates UUID v4 format (EVP key).
     */
    public boolean validateEvp(String evp) {
        return EVP_PATTERN.matcher(evp).matches();
    }

    /**
     * Validates PIX key with BACEN DICT (Diretório de Identificadores de Contas Transacionais).
     * This would integrate with BACEN's DICT API in production.
     * For now, implements format validation only.
     */
    public boolean validatePixKeyWithBacen(String pixKey) {
        // In production: Call BACEN DICT API to validate key exists and is active
        // For now: syntactic validation only
        return isValidPixKey(pixKey);
    }

    /**
     * Detects the type of PIX key.
     */
    public PixKeyType detectPixKeyType(String pixKey) {
        if (pixKey == null) return PixKeyType.UNKNOWN;

        String normalized = normalizePixKey(pixKey);

        if (CPF_PATTERN.matcher(normalized).matches()) {
            return PixKeyType.CPF;
        }
        if (CNPJ_PATTERN.matcher(normalized).matches()) {
            return PixKeyType.CNPJ;
        }
        if (EVP_PATTERN.matcher(normalized).matches()) {
            return PixKeyType.EVP;
        }
        if (EMAIL_PATTERN.matcher(normalized).matches()) {
            return PixKeyType.EMAIL;
        }
        if (PHONE_PATTERN.matcher(normalized).matches()) {
            return PixKeyType.PHONE;
        }
        
        return PixKeyType.UNKNOWN;
    }

    /**
     * Normalizes PIX key (removes spaces, converts to lowercase for email).
     */
    public String normalizePixKey(String pixKey) {
        if (pixKey == null) return "";
        // Remove all whitespace
        String normalized = pixKey.replaceAll("\\s+", "");
        // Email to lowercase
        if (normalized.contains("@")) {
            normalized = normalized.toLowerCase();
        }
        return normalized;
    }

    // -------------------------------------------------------------------------
    // Transaction Amount Validation
    // -------------------------------------------------------------------------

    /**
     * Validates PIX transaction amount against limits.
     * 
     * @param amount the requested amount
     * @param maxAmount the configured maximum
     * @return true if within limits
     */
    public boolean validateTransactionAmount(java.math.BigDecimal amount, 
                                              java.math.BigDecimal maxAmount) {
        if (amount == null || amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return false;
        }
        return amount.compareTo(maxAmount) <= 0;
    }

    // -------------------------------------------------------------------------
    // Inner Enum
    // -------------------------------------------------------------------------

    public enum PixKeyType {
        CPF,        // Individual Tax ID
        CNPJ,      // Corporate Tax ID
        PHONE,     // Mobile phone number
        EMAIL,    // Email address
        EVP,      // Endereço Virtual de Pagamento (UUID)
        UNKNOWN   // Unrecognized format
    }
}