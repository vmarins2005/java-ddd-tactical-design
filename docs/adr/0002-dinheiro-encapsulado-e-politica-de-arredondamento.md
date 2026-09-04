# ADR 0002 — Dinheiro encapsulado e política de arredondamento

Status: aceito · 2026-09-04 · supera: —

## Contexto

`BigDecimal` resolve o problema de precisão e não resolve nenhum dos outros três
problemas de dinheiro em Java:

1. **Escala.** `new BigDecimal("10.0")` e `new BigDecimal("10.00")` não são `equals`. Um
   `Map<BigDecimal, ?>` ou um `assertEquals` passa a depender de quantos zeros alguém
   digitou.
2. **Moeda.** Nada impede somar reais com dólares. O compilador vê dois `BigDecimal`.
3. **Sinal.** Nada impede um total negativo. O erro de cálculo que produziu o negativo
   fica escondido até alguém olhar um relatório.

## Decisões

### Duas casas decimais, sempre, normalizadas no construtor

`Dinheiro` representa dinheiro que alguém vai cobrar ou pagar — não resultado
intermediário de cálculo. Meio centavo não existe.

A normalização no construtor conserta o `equals` do record: `Dinheiro.reais("10.0")` e
`Dinheiro.reais("10.00")` são iguais, como devem ser. O teste
`igualdadeIndependeDaEscala` documenta essa diferença em relação ao `BigDecimal` cru.

### `HALF_UP`

Arredondamento comercial, o que a maioria das pessoas espera ao ver 0,005 virar 0,01.

A alternativa séria é `HALF_EVEN` (arredondamento bancário), que não enviesa a soma de
muitos arredondamentos para cima. Ela importa quando se soma um volume grande de valores
arredondados — folha de pagamento, rateio de milhares de linhas, juros diários. Não é o
caso aqui, onde o arredondamento acontece uma vez por item.

`HALF_EVEN` volta à mesa no projeto de rateio da série, onde a invariante "a soma das
partes é igual ao total" é o assunto inteiro.

### Arredondar uma vez, no resultado

`percentual()` multiplica em precisão cheia e arredonda ao construir o `Dinheiro` de
saída. `Percentual.comoFracao()` usa dez casas justamente para não perder precisão antes
da multiplicação.

O que **não** foi feito: arredondar a cada passo intermediário de uma cadeia de descontos.
Isso acumula erro e faz o resultado depender da ordem das operações por um motivo errado.

### Valor negativo é recusado na construção

Não existe `Dinheiro` negativo, e `subtrair` recusa quando o resultado seria negativo.

Isso é mais rígido do que a maioria das implementações de Money, e é deliberado: neste
domínio, um valor negativo é sempre defeito de cálculo. Descobrir na subtração é
infinitamente mais barato que descobrir três camadas adiante, com o número errado já
gravado. A consequência prática aparece no `total()`: como o desconto do cupom é sempre
limitado ao subtotal, a conta nunca tenta subtrair a mais.

### Moeda faz parte do tipo

Toda operação binária verifica a moeda e falha com `MoedasIncompativeis`. Um pedido tem
moeda própria, definida na criação, e recusa item ou frete em moeda diferente.

## Consequências

- \+ Três classes de bug — escala, moeda misturada, valor negativo — deixam de ser
  possíveis em vez de serem procuradas em code review.
- \+ O erro aparece no ponto onde a conta está errada, com mensagem que diz os dois
  operandos.
- − Cada operação nova em dinheiro exige um método em `Dinheiro`. `BigDecimal` cru
  deixaria escrever qualquer coisa na hora.
- − Recusar negativo custa cuidado: quem escrever `a.subtrair(b)` sem garantir a ordem vai
  tomar exceção. É o preço de o modelo ser rígido, e é o comportamento desejado.
- − Não há suporte a câmbio. Converter BRL para USD exigiria uma taxa e uma data, que são
  outro conceito e não cabem neste value object.
