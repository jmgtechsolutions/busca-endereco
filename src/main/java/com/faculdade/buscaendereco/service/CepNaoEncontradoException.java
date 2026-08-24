package com.faculdade.buscaendereco.service;

/**
 * Lancada quando o CEP consultado tem formato valido, mas nao existe na base
 * do ViaCEP (a API responde {@code {"erro": "true"}}).
 */
public class CepNaoEncontradoException extends Exception {

    private static final long serialVersionUID = 1L;

    public CepNaoEncontradoException(String cep) {
        super("O CEP " + cep + " nao foi encontrado na base dos Correios.");
    }
}
