/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.providers.sinpe.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for BCCR SINPE SMS notification API.
 */
public class SinpeSmsRequest {

    @JsonProperty("telefono")
    private String telefono;

    @JsonProperty("mensaje")
    private String mensaje;

    @JsonProperty("referencia")
    private String referencia;

    public SinpeSmsRequest(String telefono, String mensaje) {
        this.telefono = telefono;
        this.mensaje = mensaje;
    }

    public SinpeSmsRequest(String telefono, String mensaje, String referencia) {
        this.telefono = telefono;
        this.mensaje = mensaje;
        this.referencia = referencia;
    }

    // Getters / Setters
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
}