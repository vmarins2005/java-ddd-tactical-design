package io.github.vmarins2005.pedido;

public class ItemInexistente extends RegraDeNegocioViolada {

    public ItemInexistente(String sku) {
        super("o pedido nao tem o item " + sku);
    }
}
