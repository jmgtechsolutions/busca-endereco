package com.faculdade.buscaendereco;

import com.faculdade.buscaendereco.console.AnimacaoCarregamento;
import com.faculdade.buscaendereco.console.Console;
import com.faculdade.buscaendereco.model.Endereco;
import com.faculdade.buscaendereco.service.CepNaoEncontradoException;
import com.faculdade.buscaendereco.service.ViaCepService;

import java.io.IOException;
import java.util.Scanner;

/**
 * Classe principal da aplicacao busca-endereco.
 *
 * Fluxo do programa:
 *
 *   1. O CEP chega por argumento de linha de comando OU e digitado no console.
 *   2. Uma THREAD SECUNDARIA comeca a desenhar a animacao ASCII no terminal.
 *   3. A THREAD PRINCIPAL faz o HTTP GET na API ViaCEP e fica bloqueada
 *      esperando a resposta.
 *   4. Assim que a resposta chega, a thread principal pede para a animacao
 *      parar, espera ela encerrar (join) e imprime o endereco formatado.
 *
 * E esse paralelismo que atende ao requisito de multithread do exercicio: sem
 * ele, a tela ficaria congelada durante toda a espera da requisicao web.
 */
public class App {

    /**
     * Tempo minimo que a animacao fica visivel.
     *
     * A API ViaCEP costuma responder em poucas centenas de milissegundos, o que
     * quase nao daria tempo de ver a animacao. Este piso existe apenas para fins
     * de demonstracao; basta trocar para 0 para exibir o tempo real da rede.
     */
    private static final long DURACAO_MINIMA_ANIMACAO_MS = 900L;

    private static final String COMANDO_SAIR = "sair";

    public static void main(String[] args) {
        Console.habilitarAnsi();

        // Garante que o cursor volte a aparecer mesmo se o usuario der Ctrl+C
        // no meio da animacao.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.print(Console.MOSTRAR_CURSOR + Console.RESET);
            System.out.flush();
        }));

        Console.limparTela();
        imprimirCabecalho();

        ViaCepService servico = new ViaCepService();

        if (args.length > 0) {
            // Modo argumento: java -jar busca-endereco.jar 74735060
            consultarEExibir(servico, args[0]);
        } else {
            // Modo interativo: o usuario digita o CEP no console.
            modoInterativo(servico);
        }
    }

    // ------------------------------------------------------------------
    // Entrada de dados
    // ------------------------------------------------------------------

    private static void modoInterativo(ViaCepService servico) {
        Console.println(Console.ESCURO,
                "  Digite um CEP para consultar ou \"" + COMANDO_SAIR + "\" para encerrar.");
        System.out.println();

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                Console.print(Console.NEGRITO + Console.VERDE, "  CEP > ");

                if (!scanner.hasNextLine()) {
                    break;
                }

                String entrada = scanner.nextLine().trim();

                if (entrada.isEmpty()) {
                    continue;
                }

                if (COMANDO_SAIR.equalsIgnoreCase(entrada)) {
                    break;
                }

                System.out.println();
                consultarEExibir(servico, entrada);
                System.out.println();
            }
        }

        Console.println(Console.CIANO, "  Ate mais!");
    }

    // ------------------------------------------------------------------
    // Consulta com animacao em thread paralela
    // ------------------------------------------------------------------

    private static void consultarEExibir(ViaCepService servico, String cepDigitado) {
        String cep;
        try {
            cep = ViaCepService.normalizar(cepDigitado);
        } catch (IllegalArgumentException e) {
            imprimirErro(e.getMessage());
            return;
        }

        try {
            Endereco endereco = buscarComAnimacao(servico, cep);
            imprimirEndereco(endereco);
        } catch (CepNaoEncontradoException e) {
            imprimirErro(e.getMessage());
        } catch (IllegalArgumentException e) {
            imprimirErro(e.getMessage());
        } catch (IOException e) {
            imprimirErro("Nao foi possivel falar com a API ViaCEP: " + e.getMessage()
                    + " (verifique sua conexao com a internet).");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            imprimirErro("A consulta foi interrompida.");
        }
    }

    /**
     * Dispara a animacao em uma thread separada, executa a requisicao HTTP na
     * thread atual e so entao encerra a animacao.
     */
    private static Endereco buscarComAnimacao(ViaCepService servico, String cep)
            throws IOException, InterruptedException, CepNaoEncontradoException {

        AnimacaoCarregamento animacao =
                new AnimacaoCarregamento("Consultando o CEP " + formatarCep(cep) + " no ViaCEP");

        // A thread da animacao e daemon: se o programa terminar, ela nao segura
        // a JVM aberta.
        Thread threadAnimacao = new Thread(animacao, "thread-animacao");
        threadAnimacao.setDaemon(true);
        threadAnimacao.start();

        long inicio = System.currentTimeMillis();

        try {
            // Chamada BLOQUEANTE: a thread principal para aqui, mas a animacao
            // continua rodando em paralelo.
            Endereco endereco = servico.buscarPorCep(cep);
            aguardarTempoMinimoDeAnimacao(inicio);
            return endereco;
        } finally {
            // Independente de sucesso ou erro, encerra a animacao e espera a
            // thread realmente terminar antes de escrever qualquer outra coisa
            // no console (senao a saida sairia embaralhada).
            animacao.parar();
            threadAnimacao.join();
        }
    }

    private static void aguardarTempoMinimoDeAnimacao(long inicio) throws InterruptedException {
        long decorrido = System.currentTimeMillis() - inicio;
        long restante = DURACAO_MINIMA_ANIMACAO_MS - decorrido;

        if (restante > 0) {
            Thread.sleep(restante);
        }
    }

    // ------------------------------------------------------------------
    // Saida formatada
    // ------------------------------------------------------------------

    private static void imprimirCabecalho() {
        Console.println(Console.CIANO + Console.NEGRITO,
                "  ============================================");
        Console.println(Console.CIANO + Console.NEGRITO,
                "        BUSCA ENDERECO  -  API ViaCEP");
        Console.println(Console.CIANO + Console.NEGRITO,
                "  ============================================");
        System.out.println();
    }

    private static void imprimirEndereco(Endereco endereco) {
        Console.println(Console.VERDE + Console.NEGRITO, "  ENDERECO ENCONTRADO");
        Console.println(Console.VERDE, "  --------------------------------------------");

        linha("CEP", endereco.getCep());
        linha("Logradouro", endereco.getLogradouro());
        linha("Complemento", endereco.getComplemento());
        linha("Unidade", endereco.getUnidade());
        linha("Bairro", endereco.getBairro());
        linha("Cidade", endereco.getLocalidade());
        linha("UF", endereco.getUf());
        linha("Estado", endereco.getEstado());
        linha("Regiao", endereco.getRegiao());
        linha("IBGE", endereco.getIbge());
        linha("GIA", endereco.getGia());
        linha("DDD", endereco.getDdd());
        linha("SIAFI", endereco.getSiafi());
    }

    /** Imprime "rotulo ....: valor", pulando campos vazios. */
    private static void linha(String rotulo, String valor) {
        if (valor == null || valor.isBlank()) {
            return;
        }

        StringBuilder pontos = new StringBuilder();
        for (int i = rotulo.length(); i < 12; i++) {
            pontos.append('.');
        }

        System.out.println("  " + Console.ESCURO + rotulo + pontos + ": " + Console.RESET
                + Console.NEGRITO + valor + Console.RESET);
    }

    private static void imprimirErro(String mensagem) {
        Console.println(Console.VERMELHO + Console.NEGRITO, "  [ERRO] " + mensagem);
    }

    /** Formata 74735060 como 74735-060, apenas para exibicao. */
    private static String formatarCep(String cep) {
        return cep.substring(0, 5) + "-" + cep.substring(5);
    }
}
