package io.github.vmarins2005.pedido;

import java.time.LocalDateTime;

public record PagamentoRegistrado(PedidoId pedidoId, Dinheiro valor, Dinheiro saldoDevedor,
                                  LocalDateTime ocorridoEm) implements EventoDeDominio {
}
