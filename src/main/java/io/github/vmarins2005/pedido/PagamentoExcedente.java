package io.github.vmarins2005.pedido;

public class PagamentoExcedente extends RegraDeNegocioViolada {

    public PagamentoExcedente(Dinheiro tentativa, Dinheiro saldoDevedor) {
        super("pagamento de %s excede o saldo devedor de %s".formatted(tentativa, saldoDevedor));
    }
}
