# ADR 0003 — Exceção de domínio em vez de retorno booleano

Status: aceito · 2026-09-04 · supera: —

## Contexto

Quando `pagar` é chamado num pedido cancelado, o agregado precisa recusar. A forma da
recusa é uma decisão de desenho com consequências reais.

## Alternativas

**1. Retornar `boolean`.**
```java
if (!pedido.pagar(valor, agora)) { /* e agora? */ }
```
O compilador não obriga ninguém a olhar o retorno. `pedido.pagar(...)` como statement
compila, e o programa segue como se tivesse funcionado. Pior: o booleano não diz **por
que** falhou — cancelado? valor acima do saldo? ainda em rascunho? Quem chama precisa
reconstruir o motivo, e normalmente repete a regra do lado de fora, que é onde ela começa
a divergir.

**2. Retornar um `Resultado<T, Erro>` (either / result type).**
Força o tratamento e carrega o motivo. É a alternativa mais forte, e em linguagens com
tipos-soma e pattern matching de erro no núcleo é provavelmente a escolha certa.
Em Java, custa: não existe no núcleo da linguagem, exige uma classe própria ou uma
biblioteca, e contamina toda assinatura de método com o tipo do erro. Em um agregado
onde **toda** violação é defeito de programa e não fluxo esperado, isso é cerimônia para
o caso raro.

**3. Lançar exceção de domínio não checada. — escolhida**

**4. Exceção checada.**
Obriga o tratamento, mas espalha `throws` por toda a cadeia de chamadas e empurra quem
chama para o `catch` vazio. Java corporativo está cheio de `catch (Exception e) {}`
nascido exatamente assim.

## Decisão

Hierarquia de exceções não checadas com raiz em `RegraDeNegocioViolada`, e uma classe por
motivo: `TransicaoInvalida`, `PagamentoExcedente`, `PrazoDeCancelamentoExpirado`,
`CupomInaplicavel`, `ValorMonetarioInvalido`, `MoedasIncompativeis`, `CpfInvalido`,
`ItemInexistente`.

Não checada porque chamar `pagar` num pedido cancelado é **defeito de programa**, não
condição esperada que todo ponto de chamada deva tratar. Quem precisa tratar — a borda
HTTP, o consumidor de fila — trata num lugar só, pela raiz comum.

Uma classe por motivo, e não uma exceção genérica com string, porque a borda precisa
mapear motivo para resposta sem interpretar texto: `TransicaoInvalida` vira 409,
`CpfInvalido` vira 400. Comparar mensagem de erro para decidir status HTTP é o tipo de
código que quebra no dia em que alguém corrige uma vírgula.

A mensagem sempre diz o estado atual e a ação recusada — `"nao e possivel pagar um pedido
em CANCELADO"` — porque a primeira pergunta de quem lê o log é sempre "em que estado
estava?".

## Consequências

- \+ Ignorar a recusa é impossível: a exceção interrompe.
- \+ A borda mapeia motivo para código HTTP por tipo, sem interpretar texto.
- \+ O teste fica direto: `assertThatThrownBy(...).isInstanceOf(PagamentoExcedente.class)`.
- − Exceção não checada não aparece na assinatura. Saber o que `pagar` pode lançar exige
  ler o código ou o Javadoc — e é por isso que o Javadoc dos comandos diz o que recusam.
- − Para fluxo de **validação de formulário**, onde se quer coletar todos os erros de uma
  vez, exceção é a ferramenta errada: ela para no primeiro. Esse caso pertence à borda,
  com Bean Validation antes de chegar ao agregado. Aqui o agregado é a última linha, não
  a primeira.
