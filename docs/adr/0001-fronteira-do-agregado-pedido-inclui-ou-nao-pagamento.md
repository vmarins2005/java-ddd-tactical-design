# ADR 0001 — A fronteira do agregado Pedido inclui pagamento

Status: aceito · 2026-09-04 · supera: —

## Contexto

A pergunta mais difícil de DDD tático não é "o que vira value object" — é **onde termina
o agregado**. A fronteira decide o que é salvo junto, o que é travado junto e qual
invariante pode ser garantida de forma imediata.

Candidatos a fazer parte do agregado `Pedido`: itens, cupom, frete, pagamentos, cliente,
produtos do catálogo, nota fiscal, entrega.

A invariante que dá a resposta é esta: **a soma dos pagamentos nunca pode passar do total
do pedido**. Se pagamento e pedido puderem ser salvos separadamente, duas requisições
simultâneas conseguem, cada uma, verificar o saldo e registrar um pagamento — e o pedido
termina pago em excesso, sem que nenhuma das duas tenha feito nada errado.

## Alternativas

**1. Pagamento como agregado próprio, referenciando o pedido por id.**
É a escolha certa quando pagamento tem ciclo de vida independente — estorno,
conciliação bancária, chargeback meses depois. Nesses cenários, o pagamento realmente é
outra coisa, com outra fronteira transacional.
O custo: a invariante "não pagar além do total" deixa de ser garantida pelo modelo e
passa a depender de um lock explícito, de uma verificação em serviço ou de aceitar
inconsistência e compensar depois.

**2. Pagamento dentro do agregado Pedido. — escolhida**
Registrar pagamento é um comando do pedido, e o pedido decide sozinho quando passa a pago.
A invariante é estrutural: `pagar` compara com `saldoDevedor()` antes de aceitar.

**3. Tudo dentro: cliente, produtos e entrega no mesmo agregado.**
Descartada sem hesitação. Cliente e produto têm vida própria e mudam por motivos
independentes do pedido. Um agregado que carrega o cliente inteiro precisa recarregá-lo e
travá-lo a cada operação de pedido, e dois pedidos do mesmo cliente passam a disputar o
mesmo registro.

## Decisão

O agregado `Pedido` inclui **itens, cupom, frete e pagamentos**.

Fora do agregado, referenciados apenas por identidade:

- **Cliente** — entra como `Cpf`, um value object. O pedido não carrega nome, endereço
  nem histórico do cliente.
- **Produto** — entra como `sku`, `descricao` e `precoUnitario` copiados no momento da
  inclusão. A cópia é deliberada: o preço do pedido é o preço praticado naquele instante,
  e uma alteração no catálogo amanhã não pode mudar o valor de um pedido de ontem.

## Consequências

- \+ "Não pagar além do total" é garantido pelo modelo, e não por disciplina de serviço.
  O teste `excedenteRecusado` prova isso sem tocar em banco.
- \+ O agregado é pequeno: carrega itens e pagamentos de um pedido, não uma árvore.
- − Um pedido com milhares de pagamentos ficaria pesado para carregar. Para o domínio
  modelado — no máximo algumas parcelas — não é problema. Se virasse, a decisão precisaria
  ser revista, e a alternativa 1 seria o caminho.
- − Estorno e conciliação bancária não cabem aqui. No dia em que couberem, pagamento
  provavelmente vira agregado próprio, e esta decisão é superada por outra.
- − A duplicação de `descricao` e `precoUnitario` entre catálogo e pedido é intencional e
  precisa estar dita, senão o próximo desenvolvedor "normaliza" isso e quebra o histórico
  de preços.
