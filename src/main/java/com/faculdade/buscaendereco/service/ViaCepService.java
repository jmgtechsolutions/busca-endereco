package com.faculdade.buscaendereco.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faculdade.buscaendereco.model.Endereco;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Responsavel por conversar com a API ViaCEP.
 *
 * Usa o {@link HttpClient} nativo do Java (disponivel a partir do Java 11),
 * portanto nao precisa de nenhuma biblioteca externa para fazer o HTTP GET.
 */
public class ViaCepService {

    private static final String URL_BASE = "https://viacep.com.br/ws/%s/json/";

    private static final Duration TIMEOUT_CONEXAO = Duration.ofSeconds(10);
    private static final Duration TIMEOUT_RESPOSTA = Duration.ofSeconds(15);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ViaCepService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT_CONEXAO)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Remove tudo que nao for digito e valida o tamanho do CEP.
     *
     * @param cepDigitado texto informado pelo usuario (aceita "74735-060",
     *                    "74735060", " 74735 060 ", etc)
     * @return o CEP contendo somente os 8 digitos
     * @throws IllegalArgumentException se o CEP nao tiver 8 digitos
     */
    public static String normalizar(String cepDigitado) {
        if (cepDigitado == null) {
            throw new IllegalArgumentException("Informe um CEP.");
        }

        String somenteDigitos = cepDigitado.replaceAll("\\D", "");

        if (somenteDigitos.length() != 8) {
            throw new IllegalArgumentException(
                    "CEP invalido: informe 8 digitos (exemplo: 74735060).");
        }

        return somenteDigitos;
    }

    /**
     * Executa o HTTP GET na API ViaCEP e converte o JSON em {@link Endereco}.
     *
     * Este metodo BLOQUEIA a thread que o chamou ate a resposta chegar. E
     * justamente por isso que a animacao roda em outra thread.
     *
     * @param cep CEP com 8 digitos, ja normalizado
     * @return o endereco encontrado
     */
    public Endereco buscarPorCep(String cep)
            throws IOException, InterruptedException, CepNaoEncontradoException {

        HttpRequest requisicao = HttpRequest.newBuilder()
                .uri(URI.create(String.format(URL_BASE, cep)))
                .timeout(TIMEOUT_RESPOSTA)
                .header("Accept", "application/json")
                .header("User-Agent", "busca-endereco/1.0 (trabalho academico ADS)")
                .GET()
                .build();

        HttpResponse<String> resposta =
                httpClient.send(requisicao, HttpResponse.BodyHandlers.ofString());

        // O ViaCEP devolve 400 quando o formato do CEP e invalido.
        if (resposta.statusCode() == 400) {
            throw new IllegalArgumentException(
                    "CEP em formato invalido segundo a API ViaCEP: " + cep);
        }

        if (resposta.statusCode() != 200) {
            throw new IOException("A API ViaCEP respondeu com status HTTP "
                    + resposta.statusCode() + ".");
        }

        Endereco endereco = objectMapper.readValue(resposta.body(), Endereco.class);

        // CEP inexistente: status 200, mas o corpo traz {"erro": "true"}.
        if (endereco.isNaoEncontrado()) {
            throw new CepNaoEncontradoException(cep);
        }

        return endereco;
    }
}
