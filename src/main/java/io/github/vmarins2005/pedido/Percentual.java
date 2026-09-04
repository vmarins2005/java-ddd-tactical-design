package io.github.vmarins2005.pedido;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Percentual entre 0 e 100.
 *
 * <p>Existe como tipo para acabar com a duvida mais cara de sistema de desconto: o campo
 * guarda {@code 10} ou {@code 0.10}? Aqui guarda 10, e {@link #comoFracao()} e o unico
 * lugar do sistema que divide por cem.
 */
public record Percentual(BigDecimal valor) {

    private static final BigDecimal CEM = new BigDecimal("100");

    public Percentual {
        Objects.requireNonNull(valor, "percentual e obrigatorio");
        if (valor.signum() < 0 || valor.compareTo(CEM) > 0) {
            throw new ValorMonetarioInvalido("percentual fora do intervalo 0..100: " + valor);
        }
    }

    public static Percentual de(String valor) {
        return new Percentual(new BigDecimal(valor));
    }

    public BigDecimal comoFracao() {
        return valor.divide(CEM, 10, Dinheiro.ARREDONDAMENTO);
    }

    @Override
    public String toString() {
        return valor.stripTrailingZeros().toPlainString() + "%";
    }
}
