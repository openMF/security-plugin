/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.providers.pix.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response from BACEN PIX API v2 - Query Charge Status (Consultar Cobrança).
 * Maps the GET /pix/v2/cob/{txid} response payload.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PixTransactionStatus {

    @JsonProperty("txid")
    private String txid;

    /**
     * Status: ATIVA, CONCLUIDA, REMOVIDA_PELO_USUARIO_RECEBEDOR, REMOVIDA_PELO_PSP
     */
    @JsonProperty("status")
    private String status;

    @JsonProperty("calendario")
    private Calendario calendario;

    @JsonProperty("revisao")
    private Integer revisao;

    @JsonProperty("devedor")
    private Devedor devedor;

    @JsonProperty("loc")
    private Loc loc;

    @JsonProperty("location")
    private String location;

    @JsonProperty("valor")
    private Valor valor;

    @JsonProperty("chave")
    private String chave;

    @JsonProperty("solicitacaoPagador")
    private String solicitacaoPagador;

    @JsonProperty("pixCopiaECola")
    private String pixCopiaECola;

    /**
     * List of PIX receipts (receipts) when status is CONCLUIDA.
     */
    @JsonProperty("pix")
    private List<PixReceipt> pix;

    // -------------------------------------------------------------------------
    // Nested Classes
    // -------------------------------------------------------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Calendario {
        @JsonProperty("criacao")
        private LocalDateTime criacao;

        @JsonProperty("expiracao")
        private Integer expiracao;

        @JsonProperty("apresentacao")
        private LocalDateTime apresentacao;

        public LocalDateTime getCriacao() {
            return criacao;
        }

        public void setCriacao(LocalDateTime criacao) {
            this.criacao = criacao;
        }

        public Integer getExpiracao() {
            return expiracao;
        }

        public void setExpiracao(Integer expiracao) {
            this.expiracao = expiracao;
        }

        public LocalDateTime getApresentacao() {
            return apresentacao;
        }

        public void setApresentacao(LocalDateTime apresentacao) {
            this.apresentacao = apresentacao;
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Loc {
        @JsonProperty("id")
        private Integer id;

        @JsonProperty("location")
        private String location;

        @JsonProperty("tipoCob")
        private String tipoCob;

        @JsonProperty("criacao")
        private LocalDateTime criacao;

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

        public LocalDateTime getCriacao() {
            return criacao;
        }

        public void setCriacao(LocalDateTime criacao) {
            this.criacao = criacao;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Valor {
        @JsonProperty("original")
        private String original;

        @JsonProperty("modalidadeAlteracao")
        private Integer modalidadeAlteracao;

        @JsonProperty("retirada")
        private Retirada retirada;

        public String getOriginal() {
            return original;
        }

        public void setOriginal(String original) {
            this.original = original;
        }

        public Integer getModalidadeAlteracao() {
            return modalidadeAlteracao;
        }

        public void setModalidadeAlteracao(Integer modalidadeAlteracao) {
            this.modalidadeAlteracao = modalidadeAlteracao;
        }

        public Retirada getRetirada() {
            return retirada;
        }

        public void setRetirada(Retirada retirada) {
            this.retirada = retirada;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Retirada {
        @JsonProperty("saque")
        private Saque saque;

        @JsonProperty("troco")
        private Troco troco;

        public Saque getSaque() {
            return saque;
        }

        public void setSaque(Saque saque) {
            this.saque = saque;
        }

        public Troco getTroco() {
            return troco;
        }

        public void setTroco(Troco troco) {
            this.troco = troco;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Saque {
        @JsonProperty("valor")
        private String valor;

        @JsonProperty("modalidadeAlteracao")
        private Integer modalidadeAlteracao;

        @JsonProperty("modalidadeAgente")
        private String modalidadeAgente;

        @JsonProperty("prestadorDoServicoDeSaque")
        private String prestadorDoServicoDeSaque;

        public String getValor() {
            return valor;
        }

        public void setValor(String valor) {
            this.valor = valor;
        }

        public Integer getModalidadeAlteracao() {
            return modalidadeAlteracao;
        }

        public void setModalidadeAlteracao(Integer modalidadeAlteracao) {
            this.modalidadeAlteracao = modalidadeAlteracao;
        }

        public String getModalidadeAgente() {
            return modalidadeAgente;
        }

        public void setModalidadeAgente(String modalidadeAgente) {
            this.modalidadeAgente = modalidadeAgente;
        }

        public String getPrestadorDoServicoDeSaque() {
            return prestadorDoServicoDeSaque;
        }

        public void setPrestadorDoServicoDeSaque(String prestadorDoServicoDeSaque) {
            this.prestadorDoServicoDeSaque = prestadorDoServicoDeSaque;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Troco {
        @JsonProperty("valor")
        private String valor;

        @JsonProperty("modalidadeAlteracao")
        private Integer modalidadeAlteracao;

        @JsonProperty("modalidadeAgente")
        private String modalidadeAgente;

        @JsonProperty("prestadorDoServicoDeSaque")
        private String prestadorDoServicoDeSaque;

        public String getValor() {
            return valor;
        }

        public void setValor(String valor) {
            this.valor = valor;
        }

        public Integer getModalidadeAlteracao() {
            return modalidadeAlteracao;
        }

        public void setModalidadeAlteracao(Integer modalidadeAlteracao) {
            this.modalidadeAlteracao = modalidadeAlteracao;
        }

        public String getModalidadeAgente() {
            return modalidadeAgente;
        }

        public void setModalidadeAgente(String modalidadeAgente) {
            this.modalidadeAgente = modalidadeAgente;
        }

        public String getPrestadorDoServicoDeSaque() {
            return prestadorDoServicoDeSaque;
        }

        public void setPrestadorDoServicoDeSaque(String prestadorDoServicoDeSaque) {
            this.prestadorDoServicoDeSaque = prestadorDoServicoDeSaque;
        }
    }

    /**
     * PIX receipt details when payment is completed.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PixReceipt {
        @JsonProperty("endToEndId")
        private String endToEndId;

        @JsonProperty("txid")
        private String txid;

        @JsonProperty("valor")
        private String valor;

        @JsonProperty("horario")
        private LocalDateTime horario;

        @JsonProperty("pagador")
        private Pagador pagador;

        @JsonProperty("infoPagador")
        private String infoPagador;

        @JsonProperty("devolucoes")
        private List<Devolucao> devolucoes;

        public String getEndToEndId() {
            return endToEndId;
        }

        public void setEndToEndId(String endToEndId) {
            this.endToEndId = endToEndId;
        }

        public String getTxid() {
            return txid;
        }

        public void setTxid(String txid) {
            this.txid = txid;
        }

        public String getValor() {
            return valor;
        }

        public void setValor(String valor) {
            this.valor = valor;
        }

        public LocalDateTime getHorario() {
            return horario;
        }

        public void setHorario(LocalDateTime horario) {
            this.horario = horario;
        }

        public Pagador getPagador() {
            return pagador;
        }

        public void setPagador(Pagador pagador) {
            this.pagador = pagador;
        }

        public String getInfoPagador() {
            return infoPagador;
        }

        public void setInfoPagador(String infoPagador) {
            this.infoPagador = infoPagador;
        }

        public List<Devolucao> getDevolucoes() {
            return devolucoes;
        }

        public void setDevolucoes(List<Devolucao> devolucoes) {
            this.devolucoes = devolucoes;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Pagador {
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

    /**
     * Refund (devolução) details.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Devolucao {
        @JsonProperty("id")
        private String id;

        @JsonProperty("rtrId")
        private String rtrId;

        @JsonProperty("valor")
        private String valor;

        @JsonProperty("horario")
        private Horario horario;

        @JsonProperty("status")
        private String status;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getRtrId() {
            return rtrId;
        }

        public void setRtrId(String rtrId) {
            this.rtrId = rtrId;
        }

        public String getValor() {
            return valor;
        }

        public void setValor(String valor) {
            this.valor = valor;
        }

        public Horario getHorario() {
            return horario;
        }

        public void setHorario(Horario horario) {
            this.horario = horario;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Horario {
        @JsonProperty("solicitacao")
        private LocalDateTime solicitacao;

        @JsonProperty("liquidacao")
        private LocalDateTime liquidacao;

        public LocalDateTime getSolicitacao() {
            return solicitacao;
        }

        public void setSolicitacao(LocalDateTime solicitacao) {
            this.solicitacao = solicitacao;
        }

        public LocalDateTime getLiquidacao() {
            return liquidacao;
        }

        public void setLiquidacao(LocalDateTime liquidacao) {
            this.liquidacao = liquidacao;
        }
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public String getTxid() {
        return txid;
    }

    public void setTxid(String txid) {
        this.txid = txid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Calendario getCalendario() {
        return calendario;
    }

    public void setCalendario(Calendario calendario) {
        this.calendario = calendario;
    }

    public Integer getRevisao() {
        return revisao;
    }

    public void setRevisao(Integer revisao) {
        this.revisao = revisao;
    }

    public Devedor getDevedor() {
        return devedor;
    }

    public void setDevedor(Devedor devedor) {
        this.devedor = devedor;
    }

    public Loc getLoc() {
        return loc;
    }

    public void setLoc(Loc loc) {
        this.loc = loc;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Valor getValor() {
        return valor;
    }

    public void setValor(Valor valor) {
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

    public List<PixReceipt> getPix() {
        return pix;
    }

    public void setPix(List<PixReceipt> pix) {
        this.pix = pix;
    }

    // -------------------------------------------------------------------------
    // Business Helpers
    // -------------------------------------------------------------------------

    /**
     * Checks if the PIX charge has been paid.
     */
    public boolean isPaid() {
        return "CONCLUIDA".equals(status) && pix != null && !pix.isEmpty();
    }

    /**
     * Gets the payment timestamp if completed.
     */
    public LocalDateTime getPaymentTimestamp() {
        if (pix != null && !pix.isEmpty()) {
            return pix.get(0).getHorario();
        }
        return null;
    }

    /**
     * Gets the payer name if payment completed.
     */
    public String getPayerName() {
        if (pix != null && !pix.isEmpty() && pix.get(0).getPagador() != null) {
            return pix.get(0).getPagador().getNome();
        }
        return null;
    }

    /**
     * Gets the end-to-end ID for reconciliation.
     */
    public String getEndToEndId() {
        if (pix != null && !pix.isEmpty()) {
            return pix.get(0).getEndToEndId();
        }
        return null;
    }

    /**
     * Parses the original amount as BigDecimal.
     */
    public BigDecimal getOriginalAmount() {
        if (valor != null && valor.getOriginal() != null) {
            return new BigDecimal(valor.getOriginal());
        }
        return null;
    }
}