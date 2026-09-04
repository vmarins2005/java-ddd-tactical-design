package io.github.vmarins2005.pedido;

public class CpfInvalido extends RegraDeNegocioViolada {

    public CpfInvalido(String mensagem) {
        super(mensagem);
    }
}
