package io.github.vmarins2005.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CpfTest {

    @Test
    @DisplayName("aceita CPF valido com ou sem mascara e guarda so os digitos")
    void aceitaValido() {
        assertThat(Cpf.de("529.982.247-25").valor()).isEqualTo("52998224725");
        assertThat(Cpf.de("52998224725").valor()).isEqualTo("52998224725");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "52998224724",   // ultimo digito verificador errado
            "52998224715",   // penultimo digito verificador errado
            "1234567890",    // dez digitos
            "123456789012",  // doze digitos
            "abcdefghijk"    // sem digito nenhum
    })
    @DisplayName("recusa CPF invalido")
    void recusaInvalido(String invalido) {
        assertThatThrownBy(() -> Cpf.de(invalido)).isInstanceOf(CpfInvalido.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"00000000000", "11111111111", "99999999999"})
    @DisplayName("recusa sequencia de digitos iguais, que passa no calculo mas nao existe")
    void recusaDigitosIguais(String sequencia) {
        assertThatThrownBy(() -> Cpf.de(sequencia))
                .isInstanceOf(CpfInvalido.class)
                .hasMessageContaining("iguais");
    }

    @Test
    @DisplayName("formatado devolve a mascara completa")
    void formatado() {
        assertThat(Cpf.de("52998224725").formatado()).isEqualTo("529.982.247-25");
    }

    @Test
    @DisplayName("toString e mascarado: CPF completo nao vaza por acidente em log")
    void toStringMascarado() {
        Cpf cpf = Cpf.de("52998224725");

        assertThat(cpf.toString()).isEqualTo("***.982.247-**");
        assertThat("cliente=" + cpf).doesNotContain("52998224725");
    }
}
