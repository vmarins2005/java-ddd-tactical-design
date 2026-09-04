package io.github.vmarins2005.pedido;

import java.util.Objects;

/**
 * CPF validado de verdade, com digito verificador.
 *
 * <p>Guardar CPF como {@code String} deixa o sistema inteiro sem saber se aquele texto ja
 * foi validado. A pergunta "preciso validar de novo aqui?" nao tem resposta, e a resposta
 * defensiva - validar em todo lugar - espalha a regra. Com um tipo, existir e ser valido
 * sao a mesma coisa, e a validacao acontece uma vez, na fronteira.
 *
 * <p>{@link #mascarado()} existe para log: CPF completo em arquivo de log e vazamento de
 * dado pessoal, e o momento de decidir isso e agora, nao depois do incidente.
 */
public record Cpf(String valor) {

    public Cpf {
        Objects.requireNonNull(valor, "CPF e obrigatorio");
        valor = apenasDigitos(valor);
        if (valor.length() != 11) {
            throw new CpfInvalido("CPF deve ter 11 digitos");
        }
        if (todosDigitosIguais(valor)) {
            throw new CpfInvalido("CPF com todos os digitos iguais e invalido");
        }
        if (digitoVerificador(valor, 9) != caractereComoInt(valor, 9)
                || digitoVerificador(valor, 10) != caractereComoInt(valor, 10)) {
            throw new CpfInvalido("digito verificador do CPF nao confere");
        }
    }

    public static Cpf de(String valor) {
        return new Cpf(valor);
    }

    public String formatado() {
        return "%s.%s.%s-%s".formatted(
                valor.substring(0, 3), valor.substring(3, 6), valor.substring(6, 9), valor.substring(9));
    }

    public String mascarado() {
        return "***.%s.%s-**".formatted(valor.substring(3, 6), valor.substring(6, 9));
    }

    private static String apenasDigitos(String texto) {
        return texto.replaceAll("\\D", "");
    }

    private static boolean todosDigitosIguais(String digitos) {
        return digitos.chars().distinct().count() == 1;
    }

    private static int digitoVerificador(String digitos, int posicao) {
        int soma = 0;
        int peso = posicao + 1;
        for (int i = 0; i < posicao; i++) {
            soma += caractereComoInt(digitos, i) * peso--;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static int caractereComoInt(String digitos, int posicao) {
        return digitos.charAt(posicao) - '0';
    }

    /**
     * Mascarado tambem no {@code toString}: assim um CPF completo nao vaza por acidente
     * numa concatenacao de log ou numa mensagem de erro.
     */
    @Override
    public String toString() {
        return mascarado();
    }
}
