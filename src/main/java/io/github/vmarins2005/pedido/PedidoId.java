package io.github.vmarins2005.pedido;

import java.util.Objects;
import java.util.UUID;

public record PedidoId(UUID valor) {

    public PedidoId {
        Objects.requireNonNull(valor, "id do pedido e obrigatorio");
    }

    public static PedidoId novo() {
        return new PedidoId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
