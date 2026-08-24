package com.faculdade.buscaendereco.console;

/**
 * Animacao ASCII exibida no console enquanto a requisicao HTTP esta em
 * andamento.
 *
 * Esta classe implementa {@link Runnable} porque ela roda em uma THREAD
 * SEPARADA da thread principal. Enquanto a thread principal fica bloqueada
 * esperando a resposta do ViaCEP, esta thread continua desenhando quadros no
 * terminal, dando ao usuario a sensacao de que o programa nao travou.
 *
 * O desenho usa {@code System.out.print} (e nunca {@code println}), combinado
 * com os codigos ANSI de "voltar ao inicio da linha" e "limpar linha". Assim
 * todos os quadros sao pintados sempre na MESMA linha, em vez de empilhar
 * centenas de linhas na tela.
 */
public class AnimacaoCarregamento implements Runnable {

    /** Quadros do "spinner". Sao caracteres ASCII puros, seguros em qualquer terminal. */
    private static final char[] QUADROS = {'|', '/', '-', '\\'};

    /** Cores que se alternam para dar vida a animacao. */
    private static final String[] CORES = {
            Console.CIANO, Console.AZUL, Console.MAGENTA, Console.AMARELO
    };

    /** Largura da barra de progresso desenhada ao lado do spinner. */
    private static final int LARGURA_BARRA = 20;

    /** Intervalo entre um quadro e outro, em milissegundos. */
    private static final long INTERVALO_MS = 90L;

    /**
     * Sinaliza se a animacao deve continuar rodando.
     *
     * O modificador {@code volatile} e essencial aqui: ele garante que a
     * alteracao feita pela thread principal (ao chamar {@link #parar()}) fique
     * imediatamente visivel para esta thread, sem ficar presa em cache de CPU.
     */
    private volatile boolean rodando = true;

    /** Mensagem exibida ao lado da animacao. */
    private final String mensagem;

    public AnimacaoCarregamento(String mensagem) {
        this.mensagem = mensagem;
    }

    /**
     * Pede educadamente para a animacao parar no proximo quadro.
     * Chamado pela thread principal assim que a resposta HTTP chega.
     */
    public void parar() {
        this.rodando = false;
    }

    @Override
    public void run() {
        int quadro = 0;
        long inicio = System.currentTimeMillis();

        System.out.print(Console.ESCONDER_CURSOR);

        try {
            while (rodando) {
                desenharQuadro(quadro, System.currentTimeMillis() - inicio);
                quadro++;

                try {
                    Thread.sleep(INTERVALO_MS);
                } catch (InterruptedException e) {
                    // Boa pratica: restaura o status de interrupcao e encerra.
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        } finally {
            // Independente de como a animacao terminou, a linha e limpa e o
            // cursor volta a aparecer, para nao sujar a saida final.
            Console.limparLinhaAtual();
            System.out.print(Console.MOSTRAR_CURSOR);
            System.out.flush();
        }
    }

    /**
     * Desenha um unico quadro da animacao, sempre na mesma linha do terminal.
     */
    private void desenharQuadro(int quadro, long decorridoMs) {
        char spinner = QUADROS[quadro % QUADROS.length];
        String cor = CORES[(quadro / 4) % CORES.length];

        // A "cabeca" da barra vai e volta, criando um efeito de vaivem.
        int ciclo = 2 * (LARGURA_BARRA - 1);
        int posicao = quadro % ciclo;
        if (posicao >= LARGURA_BARRA) {
            posicao = ciclo - posicao;
        }

        StringBuilder barra = new StringBuilder(LARGURA_BARRA);
        for (int i = 0; i < LARGURA_BARRA; i++) {
            if (i == posicao) {
                barra.append('#');
            } else if (Math.abs(i - posicao) == 1) {
                barra.append('=');
            } else {
                barra.append('.');
            }
        }

        String segundos = String.format("%.1fs", decorridoMs / 1000.0);

        // print (nao println) + \r + limpar linha = animacao no mesmo lugar.
        System.out.print(Console.INICIO_DA_LINHA
                + Console.LIMPAR_LINHA
                + cor + Console.NEGRITO + spinner + Console.RESET
                + " " + cor + mensagem + Console.RESET
                + " " + Console.ESCURO + "[" + Console.RESET
                + cor + barra + Console.RESET
                + Console.ESCURO + "]" + Console.RESET
                + " " + Console.ESCURO + segundos + Console.RESET);
        System.out.flush();
    }
}
