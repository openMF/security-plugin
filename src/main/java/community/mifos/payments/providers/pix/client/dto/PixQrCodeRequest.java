/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.providers.pix.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Request body for BACEN PIX API v2 - Create Charge (Cobrança).
 * Maps to the POST /pix/v2/cob endpoint payload.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PixQrCodeRequest {

    @JsonProperty("calendario")
    private Calendario calendario;

    @JsonProperty("devedor")
    private Devedor devedor;

    @JsonProperty("valor")
    private Valor valor;

    /**
     * PIX key of the recipient (recebedor) - registered in DICT.
     * Can be CPF, CNPJ, phone, email, or EVP.
     */
    @JsonProperty("chave")
    private String chave;

    /**
     * Description shown to the payer (max 140 chars).
     */
    @JsonProperty("solicitacaoPagador")
    private String solicitacaoPagador;

    /**
     * Additional information key-value pairs.
     */
    @JsonProperty("infoAdicionais")
    private List<InfoAdicional> infoAdicionais;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public PixQrCodeRequest() {
    }

    /**
     * Convenience constructor matching the PixPaymentProvider usage.
     *
     * @param amount          Transaction amount
     * @param description     Payment description (solicitacaoPagador)
     * @param pixKey          Recipient PIX key (chave)
     * @param referenceCode   Internal reference (added to infoAdicionais)
     */
    public PixQrCodeRequest(BigDecimal amount,
                            String description,
                            String pixKey,
                            String referenceCode) {
        this(amount, description, pixKey, referenceCode, 86400);
    }

    /**
     * Full convenience constructor with expiry.
     *
     * @param amount          Transaction amount
     * @param description     Payment description
     * @param pixKey          Recipient PIX key
     * @param referenceCode   Internal reference code
     * @param expirySeconds   QR code expiry in seconds (default 86400 = 24h)
     */
    public PixQrCodeRequest(BigDecimal amount,
                            String description,
                            String pixKey,
                            String referenceCode,
                            int expirySeconds) {
        this.chave = pixKey;
        this.solicitacaoPagador = description != null && description.length() > 140
            ? description.substring(0, 140)
            : description;

        this.valor = new Valor(formatAmount(amount));

        this.calendario = new Calendario(expirySeconds);

        this.infoAdicionais = new ArrayList<>();
        if (referenceCode != null) {
            this.infoAdicionais.add(new InfoAdicional("Referencia", referenceCode));
        }
    }

    // -------------------------------------------------------------------------
    // Nested Classes - BACEN PIX Schema
    // -------------------------------------------------------------------------

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Calendario {
        @JsonProperty("expiracao")
        private Integer expiracao;

        public Calendario() {
        }

        public Calendario(Integer expiracao) {
            this.expiracao = expiracao;
        }

        public Integer getExpiracao() {
            return expiracao;
        }

        public void setExpiracao(Integer expiracao) {
            this.expiracao = expiracao;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Devedor {
        @JsonProperty("cpf")
        private String cpf;

        @JsonProperty("cnpj")
        private String cnpj;

        @JsonProperty("nome")
        private String nome;

        public Devedor() {
        }

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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Valor {
        /**
         * Original amount as string with 2 decimal places.
         * Example: "150.00"
         */
        @JsonProperty("original")
        private String original;

        /**
         * 0 = cannot change amount, 1 = can change amount.
         */
        @JsonProperty("modalidadeAlteracao")
        private Integer modalidadeAlteracao;

        public Valor() {
        }

        public Valor(String original) {
            this.original = original;
            this.modalidadeAlteracao = 0; // Fixed amount by default
        }

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
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class InfoAdicional {
        @JsonProperty("nome")
        private String nome;

        @JsonProperty("valor")
        private String valor;

        public InfoAdicional() {
        }

        public InfoAdicional(String nome, String valor) {
            this.nome = nome;
            this.valor = valor;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public String getValor() {
            return valor;
        }

        public void setValor(String valor) {
            this.valor = valor;
        }
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    private static String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    public void addInfoAdicional(String nome, String valor) {
        if (this.infoAdicionais == null) {
            this.infoAdicionais = new ArrayList<>();
        }
        this.infoAdicionais.add(new InfoAdicional(nome, valor));
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public Calendario getCalendario() {
        return calendario;
    }

    public void setCalendario(Calendario calendario) {
        this.calendario = calendario;
    }

    public Devedor getDevedor() {
        return devedor;
    }

    public void setDevedor(Devedor devedor) {
        this.devedor = devedor;
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

    public List<InfoAdicional> getInfoAdicionais() {
        return infoAdicionais;
    }

    public void setInfoAdicionais(List<InfoAdicional> infoAdicionais) {
        this.infoAdicionais = infoAdicionais;
    }
}