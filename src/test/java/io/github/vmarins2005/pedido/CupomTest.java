package io.github.vmarins2005.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CupomTest {

    private static final Dinheiro SUBTOTAL = Dinheiro.reais("250.00");

    @Test
    @DisplayName("percentual aplica sobre o subtotal")
    void percentual() {
        Cupom cupom = new CupomPercentual("DEZ", Percentual.de("10"), Dinheiro.zeroReais(), Dinheiro.reais("100.00"));

        assertThat(cupom.descontoPara(SUBTOTAL)).isEqualTo(Dinheiro.reais("25.00"));
    }

    @Test
    @DisplayName("o teto impede que 20% vire prejuizo num pedido grande")
    void tetoLimita() {
        Cupom cupom = new CupomPercentual("VINTE", Percentual.de("20"), Dinheiro.zeroReais(), Dinheiro.reais("30.00"));

        assertThat(cupom.descontoPara(SUBTOTAL)).isEqualTo(Dinheiro.reais("30.00"));
        assertThat(cupom.descontoPara(Dinheiro.reais("10000.00"))).isEqualTo(Dinheiro.reais("30.00"));
    }

    @Test
    @DisplayName("valor fixo nunca desconta mais que o proprio subtotal")
    void valorFixoLimitadoAoSubtotal() {
        Cupom cupom = new CupomValorFixo("TRESCENTOS", Dinheiro.reais("300.00"), Dinheiro.zeroReais());

        assertThat(cupom.descontoPara(SUBTOTAL)).isEqualTo(SUBTOTAL);
    }

    @Test
    @DisplayName("pedido minimo e verificado antes de aplicar")
    void pedidoMinimo() {
        Cupom cupom = new CupomValorFixo("FRETE50", Dinheiro.reais("50.00"), Dinheiro.reais("500.00"));

        assertThatThrownBy(() -> cupom.exigirAplicavelA(SUBTOTAL))
                .isInstanceOf(CupomInaplicavel.class)
                .hasMessageContaining("pedido minimo");
    }

    @Test
    @DisplayName("cupom que nunca desconta nada e recusado na criacao")
    void cupomInutilRecusado() {
        assertThatThrownBy(() ->
                new CupomPercentual("ZERO", Percentual.de("10"), Dinheiro.zeroReais(), Dinheiro.zeroReais()))
                .isInstanceOf(ValorMonetarioInvalido.class);

        assertThatThrownBy(() -> new CupomValorFixo("ZERO", Dinheiro.zeroReais(), Dinheiro.zeroReais()))
                .isInstanceOf(ValorMonetarioInvalido.class);
    }

    @Test
    @DisplayName("a hierarquia selada obriga o switch a tratar todos os casos, sem default")
    void switchExaustivo() {
        Cupom percentual = new CupomPercentual("A", Percentual.de("5"), Dinheiro.zeroReais(), Dinheiro.reais("10.00"));
        Cupom fixo = new CupomValorFixo("B", Dinheiro.reais("15.00"), Dinheiro.zeroReais());

        assertThat(descrever(percentual)).isEqualTo("5% ate BRL 10.00");
        assertThat(descrever(fixo)).isEqualTo("BRL 15.00 fixos");
    }

    /**
     * Sem {@code default}: adicionar um terceiro tipo de cupom quebra a compilacao aqui,
     * que e exatamente onde se quer descobrir o problema.
     */
    private static String descrever(Cupom cupom) {
        return switch (cupom) {
            case CupomPercentual percentual ->
                    "%s ate %s".formatted(percentual.percentual(), percentual.teto());
            case CupomValorFixo fixo -> "%s fixos".formatted(fixo.valor());
        };
    }
}
