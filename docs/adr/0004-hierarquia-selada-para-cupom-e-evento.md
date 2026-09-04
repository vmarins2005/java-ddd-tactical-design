# ADR 0004 — Hierarquia selada para cupom e evento

Status: aceito · 2026-09-04 · supera: —

## Contexto

Duas hierarquias no modelo têm um conjunto **conhecido e fechado** de variantes:

- `Cupom` — percentual ou valor fixo;
- `EventoDeDominio` — os quatro fatos que o pedido publica.

A pergunta é se elas devem ser interfaces abertas ou `sealed`.

## Alternativas

**1. Interface aberta.**
Qualquer um implementa, inclusive de fora do pacote. Flexível — e o problema é
exatamente esse: quem consome precisa de `default` no `switch`, e o `default` é onde o
tipo novo cai silenciosamente. O bug aparece em produção, no primeiro cupom do tipo que
ninguém tratou.

**2. Enum com comportamento.**
Funciona para cupom se as variantes não tiverem dados próprios. Não é o caso: percentual
tem percentual e teto; valor fixo tem valor. Enum forçaria campos que não se aplicam a
todas as constantes.

**3. `sealed interface` com `record` implementando. — escolhida**

## Decisão

`sealed interface Cupom permits CupomPercentual, CupomValorFixo` e
`sealed interface EventoDeDominio permits PedidoFechado, PagamentoRegistrado, PedidoPago,
PedidoCancelado`.

O ganho está no `switch` exaustivo sem `default`: adicionar `CupomFreteGratis` quebra a
**compilação** de todo lugar que ainda não o trata. O teste `switchExaustivo` existe para
demonstrar isso — e o comentário no método explica que a ausência do `default` é
deliberada, senão alguém "conserta" o aviso da IDE adicionando um.

Nomes dos eventos no particípio: `PedidoPago`, `PedidoCancelado`. Evento é fato
consumado. Nome no imperativo (`EnviarEmailDeConfirmacao`) é comando disfarçado, e acopla
o agregado à decisão de quem reage — exatamente o que o evento deveria desacoplar.

Os eventos ficam acumulados dentro do agregado e são lidos por quem o salva; o agregado
não publica nada. Publicar daqui exigiria o domínio conhecer um publicador, e a garantia
de "salvou, então publicou" continuaria não existindo. Essa garantia é o assunto do
projeto de outbox da série.

## Consequências

- \+ Variante nova é erro de compilação em todos os consumidores, e não `default`
  silencioso em produção.
- \+ `switch` sem `default` deixa a intenção explícita.
- − A hierarquia fecha no módulo: cupom definido por outro time ou por plugin não cabe. Se
  o requisito virar "parceiros definem tipos de cupom", esta decisão é superada — e aí
  provavelmente por uma tabela de regras, não por uma interface aberta.
- − `permits` é mais um lugar para lembrar de editar. É o custo de o compilador ajudar.
