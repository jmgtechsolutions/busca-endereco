# busca-endereco

Aplicação Java de console que consulta um CEP na API pública [ViaCEP](https://viacep.com.br)
e, **enquanto espera a resposta da requisição web**, exibe uma animação ASCII no
terminal usando códigos de escape ANSI.

Trabalho da disciplina de **Multithread** — Análise e Desenvolvimento de Sistemas.

---

## O que o exercício pedia e onde está resolvido

| # | Requisito | Onde está |
|---|-----------|-----------|
| 1 | Projeto Java com Maven, executado via console | `pom.xml` + `App.java` |
| 2 | Acessar `https://viacep.com.br/ws/{CEP}/json/` | `service/ViaCepService.java` |
| 3 | CEP informado no console **ou** por argumento | `App.modoInterativo()` / `App.main()` |
| 4 | Buscar as informações com um HTTP GET | `ViaCepService.buscarPorCep()` |
| 5 | Animação ASCII com `print` até a resposta chegar | `console/AnimacaoCarregamento.java` |

---

## Requisitos

- **JDK 17** ou superior
- **Maven 3.8** ou superior
- Conexão com a internet (a API ViaCEP é consultada em tempo real)

---

## Como compilar

Na raiz do projeto:

```bash
mvn clean package
```

O `maven-shade-plugin` gera um *fat jar* (com o Jackson embutido) em:

```
target/busca-endereco.jar
```

---

## Como executar

**Passando o CEP por argumento** (exatamente como no enunciado):

```bash
java -jar target/busca-endereco.jar 74735060
```

**Modo interativo** — sem argumento, o programa pergunta o CEP no console e
continua aceitando novas consultas até você digitar `sair`:

```bash
java -jar target/busca-endereco.jar
```

O CEP pode ser digitado com ou sem máscara: `74735060`, `74735-060` e
`74735 060` são todos aceitos.

---

## Exemplo de saída

Durante a requisição, esta linha fica **se movendo no lugar** (sem descer a tela),
trocando de cor a cada quatro quadros:

```
/ Consultando o CEP 74735-060 no ViaCEP [.....=#=............] 0.6s
```

Quando a resposta chega, a linha da animação é apagada e o resultado aparece:

```
  ============================================
        BUSCA ENDERECO  -  API ViaCEP
  ============================================

  ENDERECO ENCONTRADO
  --------------------------------------------
  CEP.........: 74735-060
  Logradouro..: Rua Capauam
  Bairro......: Jardim Califórnia
  Cidade......: Goiânia
  UF..........: GO
  Estado......: Goiás
  Regiao......: Centro-Oeste
  IBGE........: 5208707
  DDD.........: 62
  SIAFI.......: 9373
```

---

## Como o multithread funciona aqui

O ponto central do exercício: uma requisição HTTP é uma operação **bloqueante**.
Se ela rodasse sozinha, o terminal ficaria congelado até a resposta chegar, e o
usuário não saberia se o programa travou.

A solução usa duas threads:

```
THREAD PRINCIPAL (main)                 THREAD SECUNDÁRIA (thread-animacao)
─────────────────────────               ──────────────────────────────────
threadAnimacao.start()  ───────────────► desenha o quadro 0
                                         dorme 90 ms
httpClient.send(...)                     desenha o quadro 1
   (BLOQUEADA esperando                  dorme 90 ms
    a resposta da rede)                  desenha o quadro 2
                                         ...
resposta chegou!
animacao.parar()        ───────────────► rodando = false → sai do laço,
                                         limpa a linha e devolve o cursor
threadAnimacao.join()   ◄───────────────
   (espera a animação
    terminar de verdade)
imprime o endereço
```

Três detalhes que fazem isso funcionar corretamente:

1. **`volatile boolean rodando`** — o `volatile` garante que a alteração feita
   pela thread principal em `parar()` fique imediatamente visível para a thread
   da animação, sem ficar presa em cache de CPU. Sem ele, a animação poderia
   continuar rodando depois do pedido de parada.

2. **`join()`** — a thread principal espera a animação terminar de verdade antes
   de imprimir o resultado. Sem isso, as duas threads escreveriam no console ao
   mesmo tempo e a saída sairia embaralhada.

3. **`setDaemon(true)`** — se o programa encerrar por qualquer motivo, a thread
   da animação não segura a JVM aberta.

O bloco `finally` em `App.buscarComAnimacao()` garante que a animação seja
encerrada e a linha limpa **mesmo se a requisição lançar exceção**.

---

## Códigos ANSI usados

Todo código ANSI começa com o caractere `ESC` (27 na tabela ASCII, escrito em
Java como `\033`) seguido de `[`. O terminal não imprime esses caracteres — ele
os interpreta como comandos. Estão todos centralizados em `console/Console.java`.

| Código | Efeito |
|--------|--------|
| `\r` | volta o cursor para o início da linha (carriage return) |
| `\033[2K` | apaga a linha inteira sem descer para a próxima |
| `\033[2J\033[H` | limpa a tela e leva o cursor para o topo |
| `\033[?25l` | esconde o cursor |
| `\033[?25h` | mostra o cursor de novo |
| `\033[0m` | reseta cor e estilo |
| `\033[1m` / `\033[2m` | negrito / esmaecido |
| `\033[31m` … `\033[37m` | cores do texto (vermelho, verde, amarelo, azul, magenta, ciano, branco) |

A animação só funciona porque usa `System.out.print` — **nunca `println`**.
A combinação `\r` + `\033[2K` reposiciona e limpa a mesma linha a cada quadro,
em vez de empilhar centenas de linhas na tela.

---

## Estrutura do projeto

```
busca-endereco/
├── pom.xml
├── README.md
└── src/main/java/com/faculdade/buscaendereco/
    ├── App.java                          # ponto de entrada e orquestração das threads
    ├── console/
    │   ├── Console.java                  # constantes ANSI (cores, limpar linha, cursor)
    │   └── AnimacaoCarregamento.java     # Runnable que desenha a animação
    ├── model/
    │   └── Endereco.java                 # espelha o JSON do ViaCEP
    └── service/
        ├── ViaCepService.java            # HTTP GET + validação do CEP
        └── CepNaoEncontradoException.java
```

---

## Tratamento de erros

| Situação | Comportamento |
|----------|---------------|
| CEP com menos ou mais de 8 dígitos | mensagem de erro, sem chamar a API |
| CEP inexistente (`{"erro": "true"}`) | `CepNaoEncontradoException` com mensagem amigável |
| API responde 400 | avisa que o formato foi recusado pelo ViaCEP |
| Sem internet / timeout | avisa para verificar a conexão |
| `Ctrl+C` durante a animação | *shutdown hook* devolve o cursor ao terminal |

---

## Observações sobre o terminal

- **Windows Terminal, PowerShell, Git Bash, macOS e Linux** interpretam ANSI
  nativamente.
- No **cmd.exe antigo**, se aparecerem símbolos como `←[36m` em vez de cores,
  rode no Windows Terminal.
- Se os acentos saírem trocados no Windows, execute `chcp 65001` antes de rodar.

A constante `DURACAO_MINIMA_ANIMACAO_MS` em `App.java` mantém a animação visível
por pelo menos 900 ms — o ViaCEP costuma responder em menos tempo que isso, e sem
esse piso a animação mal apareceria. Basta trocar o valor para `0` para ver o
tempo real da rede.
