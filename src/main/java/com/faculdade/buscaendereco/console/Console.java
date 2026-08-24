package com.faculdade.buscaendereco.console;

/**
 * Codigos de escape ANSI usados para controlar o terminal.
 *
 * Um codigo ANSI comeca com o caractere ESC (27 na tabela ASCII, escrito em Java
 * como a sequencia octal \\033) seguido de "[" e do comando. O terminal nao
 * imprime esses caracteres: ele os interpreta como ordens (mudar cor, limpar
 * linha, posicionar o cursor, etc).
 *
 * Referencia: https://en.wikipedia.org/wiki/ANSI_escape_code
 */
public final class Console {

    private Console() {
        // Classe utilitaria: nao deve ser instanciada.
    }

    /** Caractere ESC (ASCII 27) que inicia toda sequencia ANSI. */
    private static final String ESC = "\033[";

    // ---------------------------------------------------------------
    // Controle de cursor e limpeza
    // ---------------------------------------------------------------

    /** Volta o cursor para a coluna 1 da linha atual (carriage return). */
    public static final String INICIO_DA_LINHA = "\r";

    /** Apaga a linha inteira onde o cursor esta, sem descer para a proxima. */
    public static final String LIMPAR_LINHA = ESC + "2K";

    /** Limpa a tela inteira e leva o cursor para o canto superior esquerdo. */
    public static final String LIMPAR_TELA = ESC + "2J" + ESC + "H";

    /** Esconde o cursor (evita que ele fique piscando no meio da animacao). */
    public static final String ESCONDER_CURSOR = ESC + "?25l";

    /** Mostra o cursor novamente. */
    public static final String MOSTRAR_CURSOR = ESC + "?25h";

    // ---------------------------------------------------------------
    // Cores e estilos
    // ---------------------------------------------------------------

    public static final String RESET = ESC + "0m";
    public static final String NEGRITO = ESC + "1m";
    public static final String ESCURO = ESC + "2m";

    public static final String VERMELHO = ESC + "31m";
    public static final String VERDE = ESC + "32m";
    public static final String AMARELO = ESC + "33m";
    public static final String AZUL = ESC + "34m";
    public static final String MAGENTA = ESC + "35m";
    public static final String CIANO = ESC + "36m";
    public static final String BRANCO = ESC + "37m";

    /**
     * Habilita o processamento de codigos ANSI no console do Windows.
     *
     * O Windows Terminal e o PowerShell modernos ja entendem ANSI, mas o
     * cmd.exe antigo so passa a interpretar depois que alguem escreve uma
     * sequencia ANSI nele. Escrever um RESET logo no inicio resolve isso na
     * pratica; se ainda assim aparecerem simbolos estranhos, o programa deve
     * ser executado no Windows Terminal.
     */
    public static void habilitarAnsi() {
        System.out.print(RESET);
        System.out.flush();
    }

    /** Imprime texto colorido sem quebrar a linha. */
    public static void print(String cor, String texto) {
        System.out.print(cor + texto + RESET);
        System.out.flush();
    }

    /** Imprime texto colorido quebrando a linha ao final. */
    public static void println(String cor, String texto) {
        System.out.println(cor + texto + RESET);
    }

    /** Limpa a tela e reposiciona o cursor no topo. */
    public static void limparTela() {
        System.out.print(LIMPAR_TELA);
        System.out.flush();
    }

    /** Apaga a linha atual e devolve o cursor para o comeco dela. */
    public static void limparLinhaAtual() {
        System.out.print(INICIO_DA_LINHA + LIMPAR_LINHA);
        System.out.flush();
    }
}
