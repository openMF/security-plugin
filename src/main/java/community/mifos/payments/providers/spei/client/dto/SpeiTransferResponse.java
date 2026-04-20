/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.providers.spei.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response from Banco de México SPEI / CoDi transfer API.
 * Maps POST /spei/v1/ordenes and GET /spei/v1/ordenes/{idOrden} response payloads.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpeiTransferResponse {

    @JsonProperty("idOrden")
    private String idOrden;

    @JsonProperty("referenciaNumerica")
    private String referenciaNumerica;

    /**
     * Status: PENDIENTE, REGISTRADA, LIQUIDADA, COMPLETADA, RECHAZADA, CANCELADA, DEVUELTA
     */
    @JsonProperty("estado")
    private String estado;

    @JsonProperty("monto")
    private BigDecimal monto;

    @JsonProperty("moneda")
    private String moneda;

    @JsonProperty("claveRastreo")
    private String claveRastreo;

    @JsonProperty("nombreBeneficiario")
    private String nombreBeneficiario;

    @JsonProperty("conceptoPago")
    private String conceptoPago;

    @JsonProperty("institucionOperante")
    private String institucionOperante;

    @JsonProperty("institucionContraparte")
    private String institucionContraparte;

    @JsonProperty("fechaRegistro")
    private LocalDateTime fechaRegistro;

    @JsonProperty("fechaLiquidacion")
    private LocalDateTime fechaLiquidacion;

    @JsonProperty("causaDevolucion")
    private String causaDevolucion;

    @JsonProperty("codigoError")
    private String codigoError;

    @JsonProperty("mensajeError")
    private String mensajeError;

    /**
     * CoDi QR data (if CoDi payment).
     */
    @JsonProperty("codigoQr")
    private String codigoQr;

    /**
     * CoDi payment URL.
     */
    @JsonProperty("urlCoDi")
    private String urlCoDi;

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public String getIdOrden() {
        return idOrden;
    }

    public void setIdOrden(String idOrden) {
        this.idOrden = idOrden;
    }

    public String getReferenciaNumerica() {
        return referenciaNumerica;
    }

    public void setReferenciaNumerica(String referenciaNumerica) {
        this.referenciaNumerica = referenciaNumerica;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
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

    public String getInstitucionContraparte() {
        return institucionContraparte;
    }

    public void setInstitucionContraparte(String institucionContraparte) {
        this.institucionContraparte = institucionContraparte;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public LocalDateTime getFechaLiquidacion() {
        return fechaLiquidacion;
    }

    public void setFechaLiquidacion(LocalDateTime fechaLiquidacion) {
        this.fechaLiquidacion = fechaLiquidacion;
    }

    public String getCausaDevolucion() {
        return causaDevolucion;
    }

    public void setCausaDevolucion(String causaDevolucion) {
        this.causaDevolucion = causaDevolucion;
    }

    public String getCodigoError() {
        return codigoError;
    }

    public void setCodigoError(String codigoError) {
        this.codigoError = codigoError;
    }

    public String getMensajeError() {
        return mensajeError;
    }

    public void setMensajeError(String mensajeError) {
        this.mensajeError = mensajeError;
    }

    public String getCodigoQr() {
        return codigoQr;
    }

    public void setCodigoQr(String codigoQr) {
        this.codigoQr = codigoQr;
    }

    public String getUrlCoDi() {
        return urlCoDi;
    }

    public void setUrlCoDi(String urlCoDi) {
        this.urlCoDi = urlCoDi;
    }

    // -------------------------------------------------------------------------
    // Business Helpers
    // -------------------------------------------------------------------------

    public boolean isSuccessful() {
        return "LIQUIDADA".equalsIgnoreCase(estado) || "COMPLETADA".equalsIgnoreCase(estado);
    }

    public boolean isPending() {
        return "PENDIENTE".equalsIgnoreCase(estado) || "REGISTRADA".equalsIgnoreCase(estado);
    }

    public boolean isRejected() {
        return "RECHAZADA".equalsIgnoreCase(estado) || "CANCELADA".equalsIgnoreCase(estado);
    }

    public boolean hasError() {
        return codigoError != null && !codigoError.isBlank();
    }

    public boolean isCoDi() {
        return codigoQr != null && !codigoQr.isBlank();
    }
}