package io.github.vmarins2005.pedido;

import java.util.Objects;

/**
 * Desconto percentual com teto. O teto e o que impede um cupom de 20% de virar um
 * prejuizo de milhares de reais no pedido de um cliente corporativo.
 */
public record CupomPercentual(String codigo, Percentual percentual, Dinheiro pedidoMinimo, Dinheiro teto)
        implements Cupom {

    public CupomPercentual {
        Objects.requireNonNull(codigo, "codigo do cupom e obrigatorio");
        Objects.requireNonNull(percentual, "percentual e obrigatorio");
        Objects.requireNonNull(pedidoMinimo, "pedido minimo e obrigatorio");
        Objects.requireNonNull(teto, "teto de desconto e obrigatorio");
        if (teto.ehZero()) {
            throw new ValorMonetarioInvalido("cupom com teto zero nunca desconta nada: " + codigo);
        }
    }

    @Override
    public Dinheiro descontoPara(Dinheiro subtotal) {
        return subtotal.percentual(percentual).limitadoA(teto).limitadoA(subtotal);
    }
}
