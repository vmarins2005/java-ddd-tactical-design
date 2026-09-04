package io.github.vmarins2005.pedido;

import java.time.LocalDateTime;

public record PedidoFechado(PedidoId pedidoId, Dinheiro total, LocalDateTime ocorridoEm)
        implements EventoDeDominio {
}
