/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.providers.sinpe.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response from BCCR SINPE Móvil / SINPE transfer API.
 * Maps POST /sinpe/v1/transferencias response payload.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SinpeMobileResponse {

    @JsonProperty("idTransaccion")
    private String idTransaccion;

    @JsonProperty("numeroComprobante")
    private String numeroComprobante;

    /**
     * Status: PENDIENTE, PROCESANDO, COMPLETADA, LIQUIDADA, FALLIDA, RECHAZADA, REVERSADA
     */
    @JsonProperty("estado")
    private String estado;

    @JsonProperty("nombreReceptor")
    private String nombreReceptor;

    @JsonProperty("monto")
    private BigDecimal monto;

    @JsonProperty("moneda")
    private String moneda;

    @JsonProperty("fechaLiquidacion")
    private LocalDateTime fechaLiquidacion;

    @JsonProperty("fechaCreacion")
    private LocalDateTime fechaCreacion;

    @JsonProperty("motivo")
    private String motivo;

    @JsonProperty("cuentaCliente")
    private String cuentaCliente;

    @JsonProperty("tipoTransferencia")
    private String tipoTransferencia;

    @JsonProperty("codigoError")
    private String codigoError;

    @JsonProperty("mensajeError")
    private String mensajeError;

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public String getIdTransaccion() {
        return idTransaccion;
    }

    public void setIdTransaccion(String idTransaccion) {
        this.idTransaccion = idTransaccion;
    }

    public String getNumeroComprobante() {
        return numeroComprobante;
    }

    public void setNumeroComprobante(String numeroComprobante) {
        this.numeroComprobante = numeroComprobante;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getNombreReceptor() {
        return nombreReceptor;
    }

    public void setNombreReceptor(String nombreReceptor) {
        this.nombreReceptor = nombreReceptor;
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

    public LocalDateTime getFechaLiquidacion() {
        return fechaLiquidacion;
    }

    public void setFechaLiquidacion(LocalDateTime fechaLiquidacion) {
        this.fechaLiquidacion = fechaLiquidacion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getCuentaCliente() {
        return cuentaCliente;
    }

    public void setCuentaCliente(String cuentaCliente) {
        this.cuentaCliente = cuentaCliente;
    }

    public String getTipoTransferencia() {
        return tipoTransferencia;
    }

    public void setTipoTransferencia(String tipoTransferencia) {
        this.tipoTransferencia = tipoTransferencia;
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

    // -------------------------------------------------------------------------
    // Business Helpers
    // -------------------------------------------------------------------------

    public boolean isSuccessful() {
        return "COMPLETADA".equalsIgnoreCase(estado) || "LIQUIDADA".equalsIgnoreCase(estado);
    }

    public boolean isPending() {
        return "PENDIENTE".equalsIgnoreCase(estado) || "PROCESANDO".equalsIgnoreCase(estado);
    }

    public boolean hasError() {
        return codigoError != null && !codigoError.isBlank();
    }
}