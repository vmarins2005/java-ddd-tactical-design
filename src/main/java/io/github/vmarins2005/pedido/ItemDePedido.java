package io.github.vmarins2005.pedido;

import java.util.Objects;

/**
 * Entidade interna ao agregado: so existe dentro de um {@link Pedido} e nunca e
 * manipulada de fora. Quem quiser mudar a quantidade fala com o pedido, e nao com o item.
 */
public record ItemDePedido(String sku, String descricao, Dinheiro precoUnitario, Quantidade quantidade) {

    public ItemDePedido {
        Objects.requireNonNull(sku, "sku e obrigatorio");
        Objects.requireNonNull(descricao, "descricao e obrigatoria");
        Objects.requireNonNull(precoUnitario, "preco unitario e obrigatorio");
        Objects.requireNonNull(quantidade, "quantidade e obrigatoria");
        if (sku.isBlank()) {
            throw new ValorMonetarioInvalido("sku nao pode ser vazio");
        }
        if (precoUnitario.ehZero()) {
            throw new ValorMonetarioInvalido("item com preco zero nao pode entrar no pedido: " + sku);
        }
    }

    public Dinheiro subtotal() {
        return precoUnitario.multiplicar(quantidade);
    }

    ItemDePedido comQuantidadeSomada(Quantidade adicional) {
        return new ItemDePedido(sku, descricao, precoUnitario, quantidade.somar(adicional));
    }
}
