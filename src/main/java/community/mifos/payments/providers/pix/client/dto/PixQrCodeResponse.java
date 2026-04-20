/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.providers.pix.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from BACEN PIX API v2 - Create Charge (Cobrança).
 * Maps the POST /pix/v2/cob response payload.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PixQrCodeResponse {

    @JsonProperty("txid")
    private String txid;

    @JsonProperty("location")
    private String location;

    @JsonProperty("loc")
    private Loc loc;

    @JsonProperty("status")
    private String status;

    @JsonProperty("calendario")
    private PixQrCodeRequest.Calendario calendario;

    @JsonProperty("devedor")
    private Devedor devedor;

    @JsonProperty("valor")
    private PixQrCodeRequest.Valor valor;

    @JsonProperty("chave")
    private String chave;

    @JsonProperty("solicitacaoPagador")
    private String solicitacaoPagador;

    @JsonProperty("pixCopiaECola")
    private String pixCopiaECola;

    // QR Code payload (EMVCo BR Code)
    @JsonProperty("payload")
    private String payload;

    @JsonProperty("revisao")
    private Integer revisao;

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public String getTxid() {
        return txid;
    }

    public void setTxid(String txid) {
        this.txid = txid;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Loc getLoc() {
        return loc;
    }

    public void setLoc(Loc loc) {
        this.loc = loc;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public PixQrCodeRequest.Calendario getCalendario() {
        return calendario;
    }

    public void setCalendario(PixQrCodeRequest.Calendario calendario) {
        this.calendario = calendario;
    }

    public Devedor getDevedor() {
        return devedor;
    }

    public void setDevedor(Devedor devedor) {
        this.devedor = devedor;
    }

    public PixQrCodeRequest.Valor getValor() {
        return valor;
    }

    public void setValor(PixQrCodeRequest.Valor valor) {
        this.valor = valor;
    }

    public String getChave() {
        return chave;
    }

    public void setChave(String chave) {
        this.chave = chave;
    }

    public String getSolicitacaoPagador() {
        return solicitacaoPagador;
    }

    public void setSolicitacaoPagador(String solicitacaoPagador) {
        this.solicitacaoPagador = solicitacaoPagador;
    }

    public String getPixCopiaECola() {
        return pixCopiaECola;
    }

    public void setPixCopiaECola(String pixCopiaECola) {
        this.pixCopiaECola = pixCopiaECola;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Integer getRevisao() {
        return revisao;
    }

    public void setRevisao(Integer revisao) {
        this.revisao = revisao;
    }

    // -------------------------------------------------------------------------
    // Nested Classes
    // -------------------------------------------------------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Loc {
        @JsonProperty("id")
        private Integer id;

        @JsonProperty("location")
        private String location;

        @JsonProperty("tipoCob")
        private String tipoCob;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getTipoCob() {
            return tipoCob;
        }

        public void setTipoCob(String tipoCob) {
            this.tipoCob = tipoCob;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Devedor {
        @JsonProperty("cpf")
        private String cpf;

        @JsonProperty("cnpj")
        private String cnpj;

        @JsonProperty("nome")
        private String nome;

        public String getCpf() {
            return cpf;
        }

        public void setCpf(String cpf) {
            this.cpf = cpf;
        }

        public String getCnpj() {
            return cnpj;
        }

        public void setCnpj(String cnpj) {
            this.cnpj = cnpj;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }
    }
}