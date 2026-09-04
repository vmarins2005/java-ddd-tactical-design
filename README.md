# Pedido — DDD tático sem framework

Projeto de estudo: **agregado, value objects, invariantes e eventos de domínio**, com
zero dependências de produção. O critério de sucesso é objetivo — não existe sequência de
chamadas públicas que produza um pedido em estado incoerente, e há um teste para cada
tentativa.

Terceiro de uma série em que cada repositório isola um conceito.

## O que este projeto prova

- Value objects que tornam classes inteiras de bug **impossíveis**, em vez de procuradas
  em code review.
- Agregado sem setter: o estado só muda por verbo de negócio, e cada verbo recusa o que
  não pode.
- Onde termina o agregado, e por quê — a pergunta difícil de DDD tático.
- Eventos de domínio acumulados pelo agregado, sem infraestrutura de publicação.

## Como rodar

```bash
./mvnw test
```

56 testes. Sem Maven instalado (wrapper versionado), sem banco, sem container, sem Spring.
Só JDK 21. A suíte roda em menos de um segundo.

## Os value objects

| Tipo | O que impede |
| --- | --- |
| `Dinheiro` | escala inconsistente (`10.0` ≠ `10.00`), soma de BRL com USD, valor negativo |
| `Cpf` | texto não validado circulando pelo sistema; CPF completo vazando em log |
| `Percentual` | a dúvida "o campo guarda 10 ou 0.10?" — guarda 10, e um único lugar divide por cem |
| `Quantidade` | quantidade zero ou negativa |

`Cpf` valida dígito verificador de verdade, e o `toString` é mascarado (`***.982.247-**`)
para que um CPF completo não vaze numa concatenação de log. O teste
`toStringMascarado` fixa esse comportamento.

`Dinheiro` recusa negativo na construção **e** na subtração. É mais rígido que a maioria
das implementações de Money, e é deliberado: neste domínio, valor negativo é sempre
defeito de cálculo, e descobrir na subtração é mais barato que descobrir três camadas
adiante — ver ADR 0002.

## O agregado

```
RASCUNHO ──fechar──> AGUARDANDO_PAGAMENTO ──pagar (quita)──> PAGO
    │                        │                                 │
    └──────────────── cancelar ─────────────────┴── cancelar (até 7 dias) ──> CANCELADO
```

| Comando | Recusa quando |
| --- | --- |
| `adicionarItem` | pedido não está em rascunho · mesmo sku com preço divergente · preço zero · moeda diferente |
| `aplicarCupom` | não é rascunho · já tem cupom · subtotal abaixo do pedido mínimo |
| `fechar` | não é rascunho · pedido sem itens |
| `pagar` | não está aguardando pagamento · valor acima do saldo devedor |
| `cancelar` | já cancelado · pago há mais de 7 dias |

Três detalhes que valem mais que o resto:

**Item repetido soma quantidade; preço divergente é recusado.** Duas linhas do mesmo sku
por preços diferentes seriam duas verdades sobre o mesmo produto dentro do mesmo pedido.

**Total zero nasce pago.** Cupom cobrindo tudo, sem frete: não faz sentido esperar
pagamento de quem não deve nada. `fechar` emite `PedidoFechado` e `PedidoPago` na
sequência.

**Pagamento acima do saldo é recusado e não deixa rastro.** Essa é a invariante que
decide a fronteira do agregado — ver ADR 0001.

## Eventos

`PedidoFechado`, `PagamentoRegistrado`, `PedidoPago`, `PedidoCancelado` — todos no
particípio, porque evento é fato consumado. Nome no imperativo (`EnviarEmail`) é comando
disfarçado, e acopla o agregado a quem reage.

O agregado **acumula** e não publica. Publicar daqui exigiria o domínio conhecer um
publicador, e a garantia de "salvou, então publicou" continuaria não existindo — essa
garantia é o assunto do projeto de outbox da série.

O teste `sequenciaCompleta` verifica a história inteira de um pedido na ordem: fechado,
pago em duas parcelas, cancelado.

## Decisões registradas

| ADR | Assunto |
| --- | --- |
| [0000](docs/adr/0000-decisoes-base-do-projeto.md) | Stack, escopo e por que nenhuma dependência |
| [0001](docs/adr/0001-fronteira-do-agregado-pedido-inclui-ou-nao-pagamento.md) | A fronteira do agregado inclui pagamento |
| [0002](docs/adr/0002-dinheiro-encapsulado-e-politica-de-arredondamento.md) | Dinheiro encapsulado e política de arredondamento |
| [0003](docs/adr/0003-excecao-de-dominio-em-vez-de-retorno-booleano.md) | Exceção de domínio em vez de retorno booleano |
| [0004](docs/adr/0004-hierarquia-selada-para-cupom-e-evento.md) | Hierarquia selada para cupom e evento |

O ADR 0001 é o que vale ler primeiro: a fronteira do agregado é a decisão que amarra
todas as outras.

## Fora do escopo, de propósito

- **Persistência.** Não há repositório nem banco. A pergunta "como isso seria salvo?" fica
  em aberto e é o assunto do projeto de arquitetura hexagonal, onde o modelo de domínio
  aparece separado da entidade JPA.
- **Casos de uso e transação.** Só o modelo.
- **Câmbio.** `Dinheiro` não converte moeda: precisaria de taxa e data, que são outro
  conceito.
- **Estorno e conciliação bancária.** Se entrassem, pagamento provavelmente viraria
  agregado próprio, e o ADR 0001 seria superado.

## Exercícios

1. **Adicione `CupomFreteGratis`.** A compilação vai quebrar no `switch` do
   `CupomTest.descrever` — de propósito. Esse é o ganho da hierarquia selada, e vale
   sentir na prática.
2. **Implemente estorno parcial.** Onde ele mora? Se a resposta te levar a "pagamento
   precisa de ciclo de vida próprio", você acabou de reencontrar sozinho a alternativa 1
   do ADR 0001.
3. **Troque `HALF_UP` por `HALF_EVEN`** e veja quais testes quebram. Depois decida qual
   está certo para um domínio de varejo, e escreva o ADR que supera o 0002.
4. **Modele entrega.** Ela entra no agregado `Pedido` ou é agregado próprio? Escreva a
   invariante que justifica sua resposta — é assim que a fronteira se decide, e não por
   gosto.

## Regras de trabalho neste repositório

- Commits atômicos: cada commit compila e passa nos testes sozinho.
- Nenhuma dependência de produção. Se algo precisar entrar, é conversa de code review.
- Toda regra nova de domínio entra com o teste que prova a recusa, e não só o caminho feliz.

## O que eu faria diferente

_A preencher depois de usar._
