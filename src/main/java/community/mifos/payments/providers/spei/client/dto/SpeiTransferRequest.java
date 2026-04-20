/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.providers.spei.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Request body for Banco de México SPEI / CoDi transfer API.
 * Maps to POST /spei/v1/ordenes endpoint (ISO 20022 pacs.008 style).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpeiTransferRequest {

    /**
     * Internal reference number (referencia numerica).
     */
    @JsonProperty("referenciaNumerica")
    private String referenciaNumerica;

    /**
     * Transfer amount.
     */
    @JsonProperty("monto")
    private BigDecimal monto;

    /**
     * Currency code (always MXN for SPEI).
     */
    @JsonProperty("moneda")
    private String moneda = "MXN";

    /**
     * Recipient CLABE (18-digit standardized bank account number).
     */
    @JsonProperty("claveRastreo")
    private String claveRastreo;

    /**
     * Recipient name (beneficiario).
     */
    @JsonProperty("nombreBeneficiario")
    private String nombreBeneficiario;

    /**
     * Transfer concept / description (concepto).
     */
    @JsonProperty("conceptoPago")
    private String conceptoPago;

    /**
     * Sender institution code (3 digits, e.g., 401 for Banorte).
     */
    @JsonProperty("institucionOperante")
    private String institucionOperante;

    /**
     * Sender CLABE or account number.
     */
    @JsonProperty("cuentaOrdenante")
    private String cuentaOrdenante;

    /**
     * Sender name.
     */
    @JsonProperty("nombreOrdenante")
    private String nombreOrdenante;

    /**
     * RFC (Tax ID) of sender.
     */
    @JsonProperty("rfcCurpOrdenante")
    private String rfcCurpOrdenante;

    /**
     * RFC (Tax ID) of recipient.
     */
    @JsonProperty("rfcCurpBeneficiario")
    private String rfcCurpBeneficiario;

    /**
     * Whether this is a CoDi (QR) payment.
     */
    @JsonProperty("esCoDi")
    private Boolean esCoDi = false;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public SpeiTransferRequest() {
    }

    /**
     * Convenience constructor matching SpeiPaymentProvider usage.
     */
    public SpeiTransferRequest(String referenciaNumerica,
                               BigDecimal monto,
                               String moneda,
                               String claveRastreo,
                               String nombreBeneficiario,
                               String conceptoPago,
                               String institucionOperante) {
        this.referenciaNumerica = referenciaNumerica;
        this.monto = monto;
        this.moneda = moneda != null ? moneda : "MXN";
        this.claveRastreo = claveRastreo;
        this.nombreBeneficiario = nombreBeneficiario;
        this.conceptoPago = conceptoPago != null && conceptoPago.length() > 100
            ? conceptoPago.substring(0, 100)
            : conceptoPago;
        this.institucionOperante = institucionOperante;
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public String getReferenciaNumerica() {
        return referenciaNumerica;
    }

    public void setReferenciaNumerica(String referenciaNumerica) {
        this.referenciaNumerica = referenciaNumerica;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public String getClaveRastreo() {
        return claveRastreo;
    }

    public void setClaveRastreo(String claveRastreo) {
        this.claveRastreo = claveRastreo;
    }

    public String getNombreBeneficiario() {
        return nombreBeneficiario;
    }

    public void setNombreBeneficiario(String nombreBeneficiario) {
        this.nombreBeneficiario = nombreBeneficiario;
    }

    public String getConceptoPago() {
        return conceptoPago;
    }

    public void setConceptoPago(String conceptoPago) {
        this.conceptoPago = conceptoPago;
    }

    public String getInstitucionOperante() {
        return institucionOperante;
    }

    public void setInstitucionOperante(String institucionOperante) {
        this.institucionOperante = institucionOperante;
    }

    public String getCuentaOrdenante() {
        return cuentaOrdenante;
    }

    public void setCuentaOrdenante(String cuentaOrdenante) {
        this.cuentaOrdenante = cuentaOrdenante;
    }

    public String getNombreOrdenante() {
        return nombreOrdenante;
    }

    public void setNombreOrdenante(String nombreOrdenante) {
        this.nombreOrdenante = nombreOrdenante;
    }

    public String getRfcCurpOrdenante() {
        return rfcCurpOrdenante;
    }

    public void setRfcCurpOrdenante(String rfcCurpOrdenante) {
        this.rfcCurpOrdenante = rfcCurpOrdenante;
    }

    public String getRfcCurpBeneficiario() {
        return rfcCurpBeneficiario;
    }

    public void setRfcCurpBeneficiario(String rfcCurpBeneficiario) {
        this.rfcCurpBeneficiario = rfcCurpBeneficiario;
    }

    public Boolean getEsCoDi() {
        return esCoDi;
    }

    public void setEsCoDi(Boolean esCoDi) {
        this.esCoDi = esCoDi;
    }
}