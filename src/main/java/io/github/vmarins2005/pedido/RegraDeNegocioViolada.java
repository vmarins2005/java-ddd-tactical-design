package io.github.vmarins2005.pedido;

/**
 * Base de todas as recusas do dominio.
 *
 * <p>O dominio recusa por excecao, e nao devolvendo {@code false}. Ver ADR 0003: um
 * booleano de retorno pode ser ignorado sem que o compilador reclame, e quando e
 * ignorado o pedido segue adiante em estado invalido. Uma excecao interrompe.
 *
 * <p>Nao e checked: quem chama {@code pagar} num pedido cancelado tem um defeito de
 * programa, nao uma condicao esperada a ser tratada em todo ponto de chamada.
 */
public abstract class RegraDeNegocioViolada extends RuntimeException {

    protected RegraDeNegocioViolada(String mensagem) {
        super(mensagem);
    }
}
