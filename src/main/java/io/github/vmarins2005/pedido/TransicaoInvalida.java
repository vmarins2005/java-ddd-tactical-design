package io.github.vmarins2005.pedido;

public class TransicaoInvalida extends RegraDeNegocioViolada {

    public TransicaoInvalida(String acao, StatusPedido statusAtual) {
        super("nao e possivel %s um pedido em %s".formatted(acao, statusAtual));
    }
}
