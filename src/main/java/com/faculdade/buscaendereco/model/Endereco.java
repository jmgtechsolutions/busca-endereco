package com.faculdade.buscaendereco.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Representa o endereco devolvido pela API ViaCEP.
 *
 * Os nomes dos atributos sao iguais aos nomes dos campos do JSON, entao o
 * Jackson consegue fazer o mapeamento automaticamente, sem precisar de
 * anotacoes campo a campo.
 *
 * A anotacao {@code @JsonIgnoreProperties(ignoreUnknown = true)} protege a
 * aplicacao: se o ViaCEP passar a devolver campos novos no futuro, o programa
 * continua funcionando em vez de lancar excecao.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Endereco {

    private String cep;
    private String logradouro;
    private String complemento;
    private String unidade;
    private String bairro;
    private String localidade;
    private String uf;
    private String estado;
    private String regiao;
    private String ibge;
    private String gia;
    private String ddd;
    private String siafi;

    /**
     * Quando o CEP nao existe, o ViaCEP responde HTTP 200 com o corpo
     * {@code {"erro": "true"}}. Este campo captura essa situacao.
     */
    private Boolean erro;

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public void setLogradouro(String logradouro) {
        this.logradouro = logradouro;
    }

    public String getComplemento() {
        return complemento;
    }

    public void setComplemento(String complemento) {
        this.complemento = complemento;
    }

    public String getUnidade() {
        return unidade;
    }

    public void setUnidade(String unidade) {
        this.unidade = unidade;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getLocalidade() {
        return localidade;
    }

    public void setLocalidade(String localidade) {
        this.localidade = localidade;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getRegiao() {
        return regiao;
    }

    public void setRegiao(String regiao) {
        this.regiao = regiao;
    }

    public String getIbge() {
        return ibge;
    }

    public void setIbge(String ibge) {
        this.ibge = ibge;
    }

    public String getGia() {
        return gia;
    }

    public void setGia(String gia) {
        this.gia = gia;
    }

    public String getDdd() {
        return ddd;
    }

    public void setDdd(String ddd) {
        this.ddd = ddd;
    }

    public String getSiafi() {
        return siafi;
    }

    public void setSiafi(String siafi) {
        this.siafi = siafi;
    }

    public Boolean getErro() {
        return erro;
    }

    public void setErro(Boolean erro) {
        this.erro = erro;
    }

    /** Indica se a API sinalizou que o CEP nao foi encontrado. */
    public boolean isNaoEncontrado() {
        return Boolean.TRUE.equals(erro);
    }

    @Override
    public String toString() {
        return "Endereco{cep='" + cep + "', logradouro='" + logradouro
                + "', bairro='" + bairro + "', localidade='" + localidade
                + "', uf='" + uf + "'}";
    }
}
