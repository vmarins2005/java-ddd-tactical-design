package io.github.vmarins2005.pedido;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Raiz do agregado.
 *
 * <p>Tudo que muda o estado passa por um verbo do negocio. Nao existe
 * {@code setStatus(PAGO)}: existe {@code pagar}, que so aceita pagamento em pedido
 * fechado, recusa valor acima do saldo devedor e decide sozinho quando o pedido passa a
 * pago. Nao ha sequencia de chamadas publicas que produza um pedido incoerente.
 *
 * <p>A fronteira do agregado inclui os itens e os pagamentos, e nao inclui o cliente nem
 * o catalogo de produtos - ver ADR 0001. Itens e pagamentos so fazem sentido dentro de um
 * pedido e mudam junto com ele; cliente e produto tem vida propria.
 *
 * <p>O tempo entra por parametro. O agregado nao chama {@code LocalDateTime.now()}: quem
 * sabe que horas sao e a camada de fora. E o que permite testar a regra de prazo de
 * cancelamento sem esperar sete dias.
 */
public class Pedido {

    public static final int PRAZO_DE_CANCELAMENTO_EM_DIAS = 7;

    private final PedidoId id;
    private final Cpf cliente;
    private final Moeda moeda;
    private final LocalDateTime criadoEm;

    private final Map<String, ItemDePedido> itens = new LinkedHashMap<>();
    private final List<Pagamento> pagamentos = new ArrayList<>();
    private final List<EventoDeDominio> eventos = new ArrayList<>();

    private StatusPedido status = StatusPedido.RASCUNHO;
    private Cupom cupom;
    private Dinheiro frete;
    private LocalDateTime fechadoEm;
    private LocalDateTime pagoEm;
    private LocalDateTime canceladoEm;

    private Pedido(PedidoId id, Cpf cliente, Moeda moeda, LocalDateTime criadoEm) {
        this.id = Objects.requireNonNull(id, "id e obrigatorio");
        this.cliente = Objects.requireNonNull(cliente, "cliente e obrigatorio");
        this.moeda = Objects.requireNonNull(moeda, "moeda e obrigatoria");
        this.criadoEm = Objects.requireNonNull(criadoEm, "data de criacao e obrigatoria");
        this.frete = Dinheiro.zero(moeda);
    }

    public static Pedido novo(PedidoId id, Cpf cliente, Moeda moeda, LocalDateTime criadoEm) {
        return new Pedido(id, cliente, moeda, criadoEm);
    }

    public static Pedido emReais(PedidoId id, Cpf cliente, LocalDateTime criadoEm) {
        return novo(id, cliente, Moeda.BRL, criadoEm);
    }

    // --- comandos ---

    /**
     * Item repetido soma quantidade em vez de duplicar linha. Preco divergente para o
     * mesmo sku e recusado: significaria duas verdades sobre o mesmo produto dentro do
     * mesmo pedido.
     */
    public void adicionarItem(String sku, String descricao, Dinheiro precoUnitario, Quantidade quantidade) {
        exigirRascunho("adicionar item");
        exigirMoedaDoPedido(precoUnitario);

        ItemDePedido existente = itens.get(sku);
        if (existente == null) {
            itens.put(sku, new ItemDePedido(sku, descricao, precoUnitario, quantidade));
            return;
        }
        if (!existente.precoUnitario().equals(precoUnitario)) {
            throw new ValorMonetarioInvalido(
                    "item %s ja esta no pedido por %s e nao pode ser adicionado por %s"
                            .formatted(sku, existente.precoUnitario(), precoUnitario));
        }
        itens.put(sku, existente.comQuantidadeSomada(quantidade));
    }

    public void removerItem(String sku) {
        exigirRascunho("remover item");
        if (itens.remove(sku) == null) {
            throw new ItemInexistente(sku);
        }
    }

    public void definirFrete(Dinheiro valor) {
        exigirRascunho("definir frete");
        exigirMoedaDoPedido(valor);
        this.frete = valor;
    }

    public void aplicarCupom(Cupom novoCupom) {
        exigirRascunho("aplicar cupom");
        Objects.requireNonNull(novoCupom, "cupom e obrigatorio");
        if (cupom != null) {
            throw new CupomInaplicavel(
                    "o pedido ja tem o cupom %s; remova antes de aplicar outro".formatted(cupom.codigo()));
        }
        novoCupom.exigirAplicavelA(subtotal());
        this.cupom = novoCupom;
    }

    public void removerCupom() {
        exigirRascunho("remover cupom");
        this.cupom = null;
    }

    /**
     * Fecha o rascunho para pagamento. Se o total ja for zero - cupom cobrindo tudo, sem
     * frete - o pedido nasce pago: cobrar zero de alguem nao e um estado que faca sentido
     * esperar.
     */
    public void fechar(LocalDateTime quando) {
        exigirRascunho("fechar");
        Objects.requireNonNull(quando, "data e obrigatoria");
        if (itens.isEmpty()) {
            throw new TransicaoInvalida("fechar sem itens", status);
        }

        status = StatusPedido.AGUARDANDO_PAGAMENTO;
        fechadoEm = quando;
        registrar(new PedidoFechado(id, total(), quando));

        if (total().ehZero()) {
            marcarComoPago(quando);
        }
    }

    public void pagar(String referencia, Dinheiro valor, LocalDateTime quando) {
        if (status != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new TransicaoInvalida("pagar", status);
        }
        exigirMoedaDoPedido(valor);
        Pagamento pagamento = new Pagamento(referencia, valor, quando);

        Dinheiro saldoAntes = saldoDevedor();
        if (valor.maiorQue(saldoAntes)) {
            throw new PagamentoExcedente(valor, saldoAntes);
        }

        pagamentos.add(pagamento);
        registrar(new PagamentoRegistrado(id, valor, saldoDevedor(), quando));

        if (saldoDevedor().ehZero()) {
            marcarComoPago(quando);
        }
    }

    /**
     * Rascunho e pedido aguardando pagamento podem ser cancelados a qualquer momento.
     * Pedido pago so dentro do prazo - e a regra que da sentido ao agregado guardar
     * {@code pagoEm}.
     */
    public void cancelar(LocalDateTime quando) {
        Objects.requireNonNull(quando, "data e obrigatoria");
        if (status == StatusPedido.CANCELADO) {
            throw new TransicaoInvalida("cancelar", status);
        }
        if (status == StatusPedido.PAGO && diasDesdeOPagamento(quando) > PRAZO_DE_CANCELAMENTO_EM_DIAS) {
            throw new PrazoDeCancelamentoExpirado(pagoEm, PRAZO_DE_CANCELAMENTO_EM_DIAS);
        }

        StatusPedido statusAnterior = status;
        status = StatusPedido.CANCELADO;
        canceladoEm = quando;
        registrar(new PedidoCancelado(id, statusAnterior, totalPago(), quando));
    }

    // --- consultas ---

    public Dinheiro subtotal() {
        return itens.values().stream()
                .map(ItemDePedido::subtotal)
                .reduce(Dinheiro.zero(moeda), Dinheiro::somar);
    }

    public Dinheiro desconto() {
        return cupom == null ? Dinheiro.zero(moeda) : cupom.descontoPara(subtotal());
    }

    public Dinheiro total() {
        return subtotal().subtrair(desconto()).somar(frete);
    }

    public Dinheiro totalPago() {
        return pagamentos.stream()
                .map(Pagamento::valor)
                .reduce(Dinheiro.zero(moeda), Dinheiro::somar);
    }

    public Dinheiro saldoDevedor() {
        return total().subtrair(totalPago());
    }

    public boolean podeSerCanceladoEm(LocalDateTime quando) {
        return switch (status) {
            case RASCUNHO, AGUARDANDO_PAGAMENTO -> true;
            case PAGO -> diasDesdeOPagamento(quando) <= PRAZO_DE_CANCELAMENTO_EM_DIAS;
            case CANCELADO -> false;
        };
    }

    /**
     * Eventos acumulados desde a ultima limpeza. Copia defensiva: quem le nao mexe na
     * lista interna do agregado.
     */
    public List<EventoDeDominio> eventos() {
        return List.copyOf(eventos);
    }

    public void limparEventos() {
        eventos.clear();
    }

    public List<ItemDePedido> itens() {
        return List.copyOf(itens.values());
    }

    public List<Pagamento> pagamentos() {
        return List.copyOf(pagamentos);
    }

    public PedidoId id() {
        return id;
    }

    public Cpf cliente() {
        return cliente;
    }

    public Moeda moeda() {
        return moeda;
    }

    public StatusPedido status() {
        return status;
    }

    public Optional<Cupom> cupom() {
        return Optional.ofNullable(cupom);
    }

    public Dinheiro frete() {
        return frete;
    }

    public LocalDateTime criadoEm() {
        return criadoEm;
    }

    public Optional<LocalDateTime> fechadoEm() {
        return Optional.ofNullable(fechadoEm);
    }

    public Optional<LocalDateTime> pagoEm() {
        return Optional.ofNullable(pagoEm);
    }

    public Optional<LocalDateTime> canceladoEm() {
        return Optional.ofNullable(canceladoEm);
    }

    // --- interno ---

    private void marcarComoPago(LocalDateTime quando) {
        status = StatusPedido.PAGO;
        pagoEm = quando;
        registrar(new PedidoPago(id, total(), quando));
    }

    private void registrar(EventoDeDominio evento) {
        eventos.add(evento);
    }

    private void exigirRascunho(String acao) {
        if (status != StatusPedido.RASCUNHO) {
            throw new TransicaoInvalida(acao, status);
        }
    }

    private void exigirMoedaDoPedido(Dinheiro valor) {
        Objects.requireNonNull(valor, "valor e obrigatorio");
        if (valor.moeda() != moeda) {
            throw new MoedasIncompativeis(moeda, valor.moeda());
        }
    }

    private long diasDesdeOPagamento(LocalDateTime quando) {
        return ChronoUnit.DAYS.between(pagoEm, quando);
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        return outro instanceof Pedido pedido && id.equals(pedido.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Pedido[%s, %s, %d itens, total %s]".formatted(id, status, itens.size(), total());
    }
}
