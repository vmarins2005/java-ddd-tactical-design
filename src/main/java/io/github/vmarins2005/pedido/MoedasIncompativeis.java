package io.github.vmarins2005.pedido;

public class MoedasIncompativeis extends RegraDeNegocioViolada {

    public MoedasIncompativeis(Moeda uma, Moeda outra) {
        super("operacao entre moedas diferentes: %s e %s".formatted(uma, outra));
    }
}
