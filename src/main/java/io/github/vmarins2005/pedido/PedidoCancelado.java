package io.github.vmarins2005.pedido;

import java.time.LocalDateTime;

public record PedidoCancelado(PedidoId pedidoId, StatusPedido statusAnterior, Dinheiro valorAReembolsar,
                              LocalDateTime ocorridoEm) implements EventoDeDominio {
}
