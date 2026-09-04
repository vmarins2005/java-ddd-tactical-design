package io.github.vmarins2005.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DinheiroTest {

    @Nested
    @DisplayName("normalizacao")
    class Normalizacao {

        @Test
        @DisplayName("sempre duas casas decimais")
        void duasCasas() {
            assertThat(Dinheiro.reais("10").valor()).isEqualByComparingTo("10.00");
            assertThat(Dinheiro.reais("10").valor().scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("10.0 e 10.00 sao o mesmo dinheiro - o que BigDecimal cru nao garante")
        void igualdadeIndependeDaEscala() {
            assertThat(new BigDecimal("10.0")).isNotEqualTo(new BigDecimal("10.00"));

            assertThat(Dinheiro.reais("10.0")).isEqualTo(Dinheiro.reais("10.00"));
        }

        @Test
        @DisplayName("arredonda meio centavo para cima")
        void arredondaMeioCentavo() {
            assertThat(Dinheiro.reais("10.005").valor()).isEqualByComparingTo("10.01");
        }
    }

    @Nested
    @DisplayName("invariantes")
    class Invariantes {

        @Test
        @DisplayName("nao existe dinheiro negativo")
        void recusaNegativo() {
            assertThatThrownBy(() -> Dinheiro.reais("-0.01"))
                    .isInstanceOf(ValorMonetarioInvalido.class)
                    .hasMessageContaining("negativo");
        }

        @Test
        @DisplayName("subtracao que daria negativo falha aqui, e nao tres camadas adiante")
        void subtracaoNegativaFalha() {
            assertThatThrownBy(() -> Dinheiro.reais("10.00").subtrair(Dinheiro.reais("10.01")))
                    .isInstanceOf(ValorMonetarioInvalido.class)
                    .hasMessageContaining("negativo");
        }

        @Test
        @DisplayName("somar moedas diferentes falha na hora, com mensagem")
        void moedasDiferentes() {
            Dinheiro real = Dinheiro.reais("10.00");
            Dinheiro dolar = new Dinheiro(new BigDecimal("10.00"), Moeda.USD);

            assertThatThrownBy(() -> real.somar(dolar))
                    .isInstanceOf(MoedasIncompativeis.class)
                    .hasMessageContaining("BRL")
                    .hasMessageContaining("USD");
        }
    }

    @Nested
    @DisplayName("operacoes")
    class Operacoes {

        @Test
        @DisplayName("soma e subtracao")
        void somaESubtracao() {
            assertThat(Dinheiro.reais("10.50").somar(Dinheiro.reais("4.50"))).isEqualTo(Dinheiro.reais("15.00"));
            assertThat(Dinheiro.reais("10.50").subtrair(Dinheiro.reais("0.50"))).isEqualTo(Dinheiro.reais("10.00"));
        }

        @Test
        @DisplayName("multiplicacao por quantidade")
        void multiplicacao() {
            assertThat(Dinheiro.reais("19.90").multiplicar(Quantidade.de(3)))
                    .isEqualTo(Dinheiro.reais("59.70"));
        }

        @Test
        @DisplayName("percentual arredonda uma vez, no resultado")
        void percentual() {
            // 10% de 33,33 = 3,333 -> 3,33
            assertThat(Dinheiro.reais("33.33").percentual(Percentual.de("10")))
                    .isEqualTo(Dinheiro.reais("3.33"));
        }

        @Test
        @DisplayName("limitadoA devolve o menor dos dois")
        void limitadoA() {
            assertThat(Dinheiro.reais("50.00").limitadoA(Dinheiro.reais("30.00")))
                    .isEqualTo(Dinheiro.reais("30.00"));
            assertThat(Dinheiro.reais("20.00").limitadoA(Dinheiro.reais("30.00")))
                    .isEqualTo(Dinheiro.reais("20.00"));
        }
    }

    @Nested
    @DisplayName("percentual")
    class PercentualTest {

        @Test
        @DisplayName("guarda 10 e nao 0.10 - a divisao por cem acontece num lugar so")
        void guardaOValorLegivel() {
            assertThat(Percentual.de("10").valor()).isEqualByComparingTo("10");
            assertThat(Percentual.de("10").comoFracao()).isEqualByComparingTo("0.1");
        }

        @Test
        @DisplayName("recusa fora do intervalo 0..100")
        void recusaForaDoIntervalo() {
            assertThatThrownBy(() -> Percentual.de("101")).isInstanceOf(ValorMonetarioInvalido.class);
            assertThatThrownBy(() -> Percentual.de("-1")).isInstanceOf(ValorMonetarioInvalido.class);
        }
    }
}
