package io.github.vmarins2005.pedido;

public record Quantidade(int valor) {

    public Quantidade {
        if (valor <= 0) {
            throw new ValorMonetarioInvalido("quantidade deve ser positiva: " + valor);
        }
    }

    public static Quantidade de(int valor) {
        return new Quantidade(valor);
    }

    public Quantidade somar(Quantidade outra) {
        return new Quantidade(valor + outra.valor);
    }

    @Override
    public String toString() {
        return Integer.toString(valor);
    }
}
