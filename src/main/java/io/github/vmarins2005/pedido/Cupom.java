package io.github.vmarins2005.pedido;

/**
 * Hierarquia selada: as duas formas de cupom que existem estao declaradas aqui, e o
 * compilador garante que nenhuma terceira aparece sem passar por este arquivo.
 *
 * <p>O ganho pratico e no {@code switch} com pattern matching: quem consumir um cupom
 * precisa tratar os dois casos, e adicionar um terceiro quebra a compilacao de todos os
 * lugares que ainda nao o conhecem. Com uma interface aberta, o mesmo erro so aparece em
 * producao, no primeiro cupom do tipo novo.
 */
public sealed interface Cupom permits CupomPercentual, CupomValorFixo {

    String codigo();

    Dinheiro pedidoMinimo();

    /**
     * Desconto para um subtotal, ja limitado ao proprio subtotal - nenhum cupom deixa o
     * pedido valer menos que zero.
     */
    Dinheiro descontoPara(Dinheiro subtotal);

    default void exigirAplicavelA(Dinheiro subtotal) {
        if (subtotal.menorQue(pedidoMinimo())) {
            throw new CupomInaplicavel(
                    "cupom %s exige pedido minimo de %s, e o subtotal e %s"
                            .formatted(codigo(), pedidoMinimo(), subtotal));
        }
    }
}
