package io.github.vmarins2005.pedido;

import java.util.Objects;

public record CupomValorFixo(String codigo, Dinheiro valor, Dinheiro pedidoMinimo) implements Cupom {

    public CupomValorFixo {
        Objects.requireNonNull(codigo, "codigo do cupom e obrigatorio");
        Objects.requireNonNull(valor, "valor do cupom e obrigatorio");
        Objects.requireNonNull(pedidoMinimo, "pedido minimo e obrigatorio");
        if (valor.ehZero()) {
            throw new ValorMonetarioInvalido("cupom de valor zero nao desconta nada: " + codigo);
        }
    }

    @Override
    public Dinheiro descontoPara(Dinheiro subtotal) {
        return valor.limitadoA(subtotal);
    }
}
