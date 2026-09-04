package io.github.vmarins2005.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * O tempo entra por parametro em todo comando do agregado, entao a regra de prazo de
 * cancelamento e testada sem esperar sete dias e sem biblioteca de manipulacao de relogio.
 */
class PedidoTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 3, 10, 14, 30);
    private static final Cpf CLIENTE = Cpf.de("529.982.247-25");

    @Nested
    @DisplayName("rascunho")
    class Rascunho {

        @Test
        @DisplayName("subtotal soma os itens")
        void subtotal() {
            assertThat(pedidoComItens().subtotal()).isEqualTo(Dinheiro.reais("250.00"));
        }

        @Test
        @DisplayName("sku repetido soma quantidade em vez de duplicar linha")
        void skuRepetidoSomaQuantidade() {
            Pedido pedido = pedidoComItens();

            pedido.adicionarItem("SKU-1", "Teclado", Dinheiro.reais("100.00"), Quantidade.de(1));

            assertThat(pedido.itens()).hasSize(2);
            assertThat(pedido.subtotal()).isEqualTo(Dinheiro.reais("350.00"));
        }

        @Test
        @DisplayName("mesmo sku por preco diferente e recusado: seriam duas verdades sobre o mesmo produto")
        void precoDivergenteRecusado() {
            Pedido pedido = pedidoComItens();

            assertThatThrownBy(() ->
                    pedido.adicionarItem("SKU-1", "Teclado", Dinheiro.reais("90.00"), Quantidade.de(1)))
                    .isInstanceOf(ValorMonetarioInvalido.class)
                    .hasMessageContaining("SKU-1");
        }

        @Test
        @DisplayName("item com preco zero nao entra")
        void itemGratuitoRecusado() {
            Pedido pedido = pedidoNovo();

            assertThatThrownBy(() ->
                    pedido.adicionarItem("BRINDE", "Adesivo", Dinheiro.zeroReais(), Quantidade.de(1)))
                    .isInstanceOf(ValorMonetarioInvalido.class);
        }

        @Test
        @DisplayName("remover item inexistente reclama em vez de silenciar")
        void removerInexistente() {
            assertThatThrownBy(() -> pedidoComItens().removerItem("SKU-404"))
                    .isInstanceOf(ItemInexistente.class);
        }

        @Test
        @DisplayName("item em moeda diferente da do pedido e recusado")
        void moedaDivergente() {
            Pedido pedido = pedidoNovo();
            Dinheiro dolares = new Dinheiro(new BigDecimal("10.00"), Moeda.USD);

            assertThatThrownBy(() -> pedido.adicionarItem("SKU-US", "Importado", dolares, Quantidade.de(1)))
                    .isInstanceOf(MoedasIncompativeis.class);
        }

        @Test
        @DisplayName("a lista de itens devolvida e copia: mexer nela nao mexe no pedido")
        void itensSaoCopia() {
            Pedido pedido = pedidoComItens();

            assertThatThrownBy(() -> pedido.itens().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("cupom")
    class ComCupom {

        @Test
        @DisplayName("percentual desconta sobre o subtotal e o frete entra depois")
        void percentual() {
            Pedido pedido = pedidoComItens();
            pedido.definirFrete(Dinheiro.reais("20.00"));

            pedido.aplicarCupom(new CupomPercentual(
                    "DEZ", Percentual.de("10"), Dinheiro.zeroReais(), Dinheiro.reais("100.00")));

            assertThat(pedido.desconto()).isEqualTo(Dinheiro.reais("25.00"));
            assertThat(pedido.total()).isEqualTo(Dinheiro.reais("245.00"));
        }

        @Test
        @DisplayName("cupom com pedido minimo acima do subtotal e recusado na aplicacao")
        void pedidoMinimo() {
            Pedido pedido = pedidoComItens();
            Cupom caro = new CupomValorFixo("MIL", Dinheiro.reais("50.00"), Dinheiro.reais("1000.00"));

            assertThatThrownBy(() -> pedido.aplicarCupom(caro)).isInstanceOf(CupomInaplicavel.class);
            assertThat(pedido.cupom()).isEmpty();
        }

        @Test
        @DisplayName("so um cupom por pedido")
        void umCupomPorPedido() {
            Pedido pedido = pedidoComItens();
            pedido.aplicarCupom(new CupomValorFixo("A", Dinheiro.reais("10.00"), Dinheiro.zeroReais()));

            assertThatThrownBy(() ->
                    pedido.aplicarCupom(new CupomValorFixo("B", Dinheiro.reais("20.00"), Dinheiro.zeroReais())))
                    .isInstanceOf(CupomInaplicavel.class)
                    .hasMessageContaining("ja tem o cupom A");
        }

        @Test
        @DisplayName("cupom maior que o pedido zera o subtotal, e nunca deixa o total negativo")
        void cupomMaiorQueOPedido() {
            Pedido pedido = pedidoComItens();

            pedido.aplicarCupom(new CupomValorFixo("TUDO", Dinheiro.reais("300.00"), Dinheiro.zeroReais()));

            assertThat(pedido.desconto()).isEqualTo(Dinheiro.reais("250.00"));
            assertThat(pedido.total()).isEqualTo(Dinheiro.zeroReais());
        }
    }

    @Nested
    @DisplayName("fechamento")
    class Fechamento {

        @Test
        @DisplayName("fechar muda o status e emite o evento com o total")
        void fecha() {
            Pedido pedido = pedidoComItens();

            pedido.fechar(AGORA);

            assertThat(pedido.status()).isEqualTo(StatusPedido.AGUARDANDO_PAGAMENTO);
            assertThat(pedido.eventos()).singleElement()
                    .isEqualTo(new PedidoFechado(pedido.id(), Dinheiro.reais("250.00"), AGORA));
        }

        @Test
        @DisplayName("pedido sem item nao fecha")
        void semItens() {
            assertThatThrownBy(() -> pedidoNovo().fechar(AGORA))
                    .isInstanceOf(TransicaoInvalida.class)
                    .hasMessageContaining("sem itens");
        }

        @Test
        @DisplayName("total zero nasce pago: nao ha o que esperar de quem nao deve nada")
        void totalZeroNascePago() {
            Pedido pedido = pedidoComItens();
            pedido.aplicarCupom(new CupomValorFixo("TUDO", Dinheiro.reais("300.00"), Dinheiro.zeroReais()));

            pedido.fechar(AGORA);

            assertThat(pedido.status()).isEqualTo(StatusPedido.PAGO);
            assertThat(pedido.eventos()).hasSize(2);
            assertThat(pedido.eventos().get(1)).isInstanceOf(PedidoPago.class);
        }

        @Test
        @DisplayName("nao se mexe em pedido fechado")
        void fechadoNaoAceitaMudanca() {
            Pedido pedido = pedidoFechado();

            assertThatThrownBy(() ->
                    pedido.adicionarItem("SKU-3", "Cabo", Dinheiro.reais("10.00"), Quantidade.de(1)))
                    .isInstanceOf(TransicaoInvalida.class);
            assertThatThrownBy(() -> pedido.definirFrete(Dinheiro.reais("5.00")))
                    .isInstanceOf(TransicaoInvalida.class);
            assertThatThrownBy(() ->
                    pedido.aplicarCupom(new CupomValorFixo("X", Dinheiro.reais("5.00"), Dinheiro.zeroReais())))
                    .isInstanceOf(TransicaoInvalida.class);
        }
    }

    @Nested
    @DisplayName("pagamento")
    class PagamentoDoPedido {

        @Test
        @DisplayName("pagamento parcial reduz o saldo e o pedido continua aguardando")
        void parcial() {
            Pedido pedido = pedidoFechado();

            pedido.pagar("PIX-1", Dinheiro.reais("100.00"), AGORA);

            assertThat(pedido.status()).isEqualTo(StatusPedido.AGUARDANDO_PAGAMENTO);
            assertThat(pedido.saldoDevedor()).isEqualTo(Dinheiro.reais("150.00"));
            assertThat(pedido.eventos().get(1))
                    .isEqualTo(new PagamentoRegistrado(
                            pedido.id(), Dinheiro.reais("100.00"), Dinheiro.reais("150.00"), AGORA));
        }

        @Test
        @DisplayName("dois parciais quitam o pedido e emitem PedidoPago")
        void quitacaoEmDuasParcelas() {
            Pedido pedido = pedidoFechado();

            pedido.pagar("PIX-1", Dinheiro.reais("100.00"), AGORA);
            pedido.pagar("PIX-2", Dinheiro.reais("150.00"), AGORA.plusHours(1));

            assertThat(pedido.status()).isEqualTo(StatusPedido.PAGO);
            assertThat(pedido.saldoDevedor()).isEqualTo(Dinheiro.zeroReais());
            assertThat(pedido.pagoEm()).contains(AGORA.plusHours(1));
            assertThat(pedido.eventos()).last().isInstanceOf(PedidoPago.class);
        }

        @Test
        @DisplayName("pagamento acima do saldo e recusado e nao deixa rastro")
        void excedenteRecusado() {
            Pedido pedido = pedidoFechado();
            pedido.pagar("PIX-1", Dinheiro.reais("200.00"), AGORA);

            assertThatThrownBy(() -> pedido.pagar("PIX-2", Dinheiro.reais("100.00"), AGORA))
                    .isInstanceOf(PagamentoExcedente.class)
                    .hasMessageContaining("50.00");

            assertThat(pedido.pagamentos()).hasSize(1);
            assertThat(pedido.saldoDevedor()).isEqualTo(Dinheiro.reais("50.00"));
        }

        @Test
        @DisplayName("rascunho nao recebe pagamento")
        void rascunhoNaoRecebePagamento() {
            assertThatThrownBy(() -> pedidoComItens().pagar("PIX-1", Dinheiro.reais("10.00"), AGORA))
                    .isInstanceOf(TransicaoInvalida.class);
        }

        @Test
        @DisplayName("pedido pago nao recebe outro pagamento")
        void pagoNaoRecebeMais() {
            Pedido pedido = pedidoPago();

            assertThatThrownBy(() -> pedido.pagar("PIX-2", Dinheiro.reais("10.00"), AGORA))
                    .isInstanceOf(TransicaoInvalida.class);
        }
    }

    @Nested
    @DisplayName("cancelamento")
    class Cancelamento {

        @Test
        @DisplayName("rascunho e pedido aguardando pagamento cancelam a qualquer momento")
        void cancelaAntesDoPagamento() {
            Pedido rascunho = pedidoComItens();
            rascunho.cancelar(AGORA.plusYears(2));
            assertThat(rascunho.status()).isEqualTo(StatusPedido.CANCELADO);

            Pedido aguardando = pedidoFechado();
            aguardando.cancelar(AGORA.plusYears(2));
            assertThat(aguardando.status()).isEqualTo(StatusPedido.CANCELADO);
        }

        @Test
        @DisplayName("pedido pago cancela dentro do prazo, informando o valor a reembolsar")
        void cancelaPagoDentroDoPrazo() {
            Pedido pedido = pedidoPago();

            pedido.cancelar(AGORA.plusDays(Pedido.PRAZO_DE_CANCELAMENTO_EM_DIAS));

            assertThat(pedido.status()).isEqualTo(StatusPedido.CANCELADO);
            assertThat(pedido.eventos()).last().isEqualTo(new PedidoCancelado(
                    pedido.id(), StatusPedido.PAGO, Dinheiro.reais("250.00"),
                    AGORA.plusDays(Pedido.PRAZO_DE_CANCELAMENTO_EM_DIAS)));
        }

        @Test
        @DisplayName("um dia depois do prazo, nao cancela mais")
        void prazoExpirado() {
            Pedido pedido = pedidoPago();
            LocalDateTime tarde = AGORA.plusDays(Pedido.PRAZO_DE_CANCELAMENTO_EM_DIAS + 1);

            assertThat(pedido.podeSerCanceladoEm(tarde)).isFalse();
            assertThatThrownBy(() -> pedido.cancelar(tarde))
                    .isInstanceOf(PrazoDeCancelamentoExpirado.class);
            assertThat(pedido.status()).isEqualTo(StatusPedido.PAGO);
        }

        @Test
        @DisplayName("cancelamento e terminal")
        void cancelamentoETerminal() {
            Pedido pedido = pedidoFechado();
            pedido.cancelar(AGORA);

            assertThatThrownBy(() -> pedido.cancelar(AGORA)).isInstanceOf(TransicaoInvalida.class);
            assertThatThrownBy(() -> pedido.pagar("PIX", Dinheiro.reais("10.00"), AGORA))
                    .isInstanceOf(TransicaoInvalida.class);
        }
    }

    @Nested
    @DisplayName("eventos")
    class Eventos {

        @Test
        @DisplayName("a sequencia de eventos conta a historia do pedido, na ordem")
        void sequenciaCompleta() {
            Pedido pedido = pedidoComItens();
            pedido.fechar(AGORA);
            pedido.pagar("PIX-1", Dinheiro.reais("100.00"), AGORA.plusMinutes(1));
            pedido.pagar("PIX-2", Dinheiro.reais("150.00"), AGORA.plusMinutes(2));
            pedido.cancelar(AGORA.plusDays(1));

            assertThat(pedido.eventos()).extracting(Object::getClass).containsExactly(
                    PedidoFechado.class,
                    PagamentoRegistrado.class,
                    PagamentoRegistrado.class,
                    PedidoPago.class,
                    PedidoCancelado.class);
        }

        @Test
        @DisplayName("quem persistiu limpa os eventos; o agregado nao publica nada sozinho")
        void limparEventos() {
            Pedido pedido = pedidoFechado();
            assertThat(pedido.eventos()).isNotEmpty();

            pedido.limparEventos();

            assertThat(pedido.eventos()).isEmpty();
        }

        @Test
        @DisplayName("a lista devolvida e copia defensiva")
        void copiaDefensiva() {
            Pedido pedido = pedidoFechado();
            List<EventoDeDominio> eventos = pedido.eventos();

            assertThatThrownBy(eventos::clear).isInstanceOf(UnsupportedOperationException.class);
            assertThat(pedido.eventos()).hasSize(1);
        }
    }

    // --- auxiliares ---

    private static Pedido pedidoNovo() {
        return Pedido.emReais(PedidoId.novo(), CLIENTE, AGORA);
    }

    private static Pedido pedidoComItens() {
        Pedido pedido = pedidoNovo();
        pedido.adicionarItem("SKU-1", "Teclado mecanico", Dinheiro.reais("100.00"), Quantidade.de(2));
        pedido.adicionarItem("SKU-2", "Mouse", Dinheiro.reais("50.00"), Quantidade.de(1));
        return pedido;
    }

    private static Pedido pedidoFechado() {
        Pedido pedido = pedidoComItens();
        pedido.fechar(AGORA);
        return pedido;
    }

    private static Pedido pedidoPago() {
        Pedido pedido = pedidoFechado();
        pedido.pagar("PIX-1", Dinheiro.reais("250.00"), AGORA);
        return pedido;
    }
}
