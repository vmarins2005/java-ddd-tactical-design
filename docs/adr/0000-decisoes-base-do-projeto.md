# ADR 0000 — Decisões base do projeto

Status: aceito · 2026-09-04 · supera: —

## Contexto

O projeto existe para exercitar DDD tático: agregado, value objects, invariantes e
eventos de domínio. O critério de sucesso é objetivo — **não deve existir sequência de
chamadas públicas que produza um pedido em estado incoerente**.

## Decisões

### 1. Nenhuma dependência de produção

Sem Spring, sem JPA, sem Lombok, sem biblioteca de validação. O `pom.xml` só declara
JUnit e AssertJ em escopo de teste.

A razão não é purismo: é que cada uma dessas dependências negocia alguma coisa com o
modelo. JPA quer construtor vazio e campos acessíveis. Lombok quer gerar setters.
Bean Validation quer que a validação seja anotação lida por um motor externo, e não
código que roda no construtor.

O agregado deste projeto não tem setter, não tem construtor vazio e valida no construtor.
Não dá para demonstrar isso com um framework negociando cada ponto.

### 2. Persistência fora do escopo

Não há repositório, não há banco, não há caso de uso. Só o modelo e os testes.

Isso deixa uma pergunta em aberto de propósito: **como este agregado seria persistido?**
`Dinheiro` como duas colunas, `Cpf` como uma, itens como tabela filha. É a discussão do
projeto de arquitetura hexagonal, onde o modelo de domínio já aparece separado da
entidade JPA. Aqui misturaria dois assuntos.

### 3. O tempo entra por parâmetro

Nenhum método do agregado chama `LocalDateTime.now()`. Todo comando recebe o instante de
quem o chama.

Isso é o que permite testar a regra de prazo de cancelamento sem esperar sete dias e sem
biblioteca de manipulação de relógio. O agregado não decide que horas são — quem decide é
a camada de fora.

### 4. Java 21, usando o que a linguagem oferece

`record` para value objects (imutabilidade e `equals` de graça), `sealed` para as
hierarquias fechadas de cupom e de evento, `switch` com pattern matching exaustivo.

Ver ADR 0004 para o que isso compra em segurança de compilação.

## Consequências

- A suíte inteira roda em menos de um segundo. Isso muda o hábito de rodar teste.
- O repositório demonstra modelagem, e não integração. Quem procurar "como salvar isso"
  não encontra aqui — e isso está dito no README.
