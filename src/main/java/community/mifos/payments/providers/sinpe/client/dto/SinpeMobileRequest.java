/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.providers.sinpe.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Request body for BCCR SINPE Móvil / SINPE transfer API.
 * Maps to POST /sinpe/v1/transferencias endpoint.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SinpeMobileRequest {

    /**
     * Internal reference number (número de comprobante).
     */
    @JsonProperty("numeroComprobante")
    private String numeroComprobante;

    /**
     * Transfer amount.
     */
    @JsonProperty("monto")
    private BigDecimal monto;

    /**
     * Transfer type: SINPE_MOVIL or SINPE (IBAN).
     */
    @JsonProperty("tipoTransferencia")
    private String tipoTransferencia;

    /**
     * Destination identifier: phone number (8 digits) for SINPE Móvil,
     * or IBAN (CR + 20 digits) for SINPE.
     */
    @JsonProperty("cuentaCliente")
    private String cuentaCliente;

    /**
     * Transfer description / memo (motivo).
     */
    @JsonProperty("motivo")
    private String motivo;

    /**
     * Sender's registered phone number for SINPE Móvil.
     */
    @JsonProperty("telefonoOrigen")
    private String telefonoOrigen;

    /**
     * Sender's IBAN for SINPE transfers.
     */
    @JsonProperty("ibanOrigen")
    private String ibanOrigen;

    /**
     * Currency code (always CRC for SINPE Móvil).
     */
    @JsonProperty("moneda")
    private String moneda = "CRC";

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public SinpeMobileRequest() {
    }

    /**
     * Convenience constructor matching SinpePaymentProvider usage.
     */
    public SinpeMobileRequest(String numeroComprobante,
                              BigDecimal monto,
                              String tipoTransferencia,
                              String cuentaCliente,
                              String motivo,
                              String telefonoOrigen,
                              String ibanOrigen) {
        this.numeroComprobante = numeroComprobante;
        this.monto = monto;
        this.tipoTransferencia = tipoTransferencia;
        this.cuentaCliente = cuentaCliente;
        this.motivo = motivo != null && motivo.length() > 140 
            ? motivo.substring(0, 140) 
            : motivo;
        this.telefonoOrigen = telefonoOrigen;
        this.ibanOrigen = ibanOrigen;
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public String getNumeroComprobante() {
        return numeroComprobante;
    }

    public void setNumeroComprobante(String numeroComprobante) {
        this.numeroComprobante = numeroComprobante;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getTipoTransferencia() {
        return tipoTransferencia;
    }

    public void setTipoTransferencia(String tipoTransferencia) {
        this.tipoTransferencia = tipoTransferencia;
    }

    public String getCuentaCliente() {
        return cuentaCliente;
    }

    public void setCuentaCliente(String cuentaCliente) {
        this.cuentaCliente = cuentaCliente;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getTelefonoOrigen() {
        return telefonoOrigen;
    }

    public void setTelefonoOrigen(String telefonoOrigen) {
        this.telefonoOrigen = telefonoOrigen;
    }

    public String getIbanOrigen() {
        return ibanOrigen;
    }

    public void setIbanOrigen(String ibanOrigen) {
        this.ibanOrigen = ibanOrigen;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }
}