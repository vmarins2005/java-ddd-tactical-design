package io.github.vmarins2005.pedido;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Valor monetario com moeda. Sempre em duas casas, nunca negativo.
 *
 * <p>Tres decisoes estao codificadas aqui e explicadas no ADR 0002:
 *
 * <ol>
 *   <li><b>Duas casas sempre.</b> Isto representa dinheiro que alguem vai cobrar, e nao
 *       um resultado intermediario de calculo. Meio centavo nao existe.</li>
 *   <li><b>Nao aceita negativo.</b> Um pedido nao tem valor negativo. Se uma subtracao
 *       daria negativo, ha um defeito no calculo, e o lugar de descobrir isso e aqui -
 *       nao tres camadas adiante, com o numero errado ja gravado.</li>
 *   <li><b>Moeda faz parte do tipo.</b> Somar BRL com USD nao compila errado: falha na
 *       hora, com mensagem. {@code BigDecimal} solto nao tem como impedir isso.</li>
 * </ol>
 *
 * <p>A normalizacao para duas casas no construtor tambem conserta o {@code equals} do
 * record: {@code new BigDecimal("10.0")} e {@code new BigDecimal("10.00")} nao sao iguais
 * para o {@code BigDecimal}, mas devem ser para dinheiro.
 */
public record Dinheiro(BigDecimal valor, Moeda moeda) implements Comparable<Dinheiro> {

    public static final int CASAS_DECIMAIS = 2;
    public static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_UP;

    public Dinheiro {
        Objects.requireNonNull(valor, "valor e obrigatorio");
        Objects.requireNonNull(moeda, "moeda e obrigatoria");
        if (valor.signum() < 0) {
            throw new ValorMonetarioInvalido("valor monetario nao pode ser negativo: " + valor);
        }
        valor = valor.setScale(CASAS_DECIMAIS, ARREDONDAMENTO);
    }

    public static Dinheiro reais(String valor) {
        return new Dinheiro(new BigDecimal(valor), Moeda.BRL);
    }

    public static Dinheiro zero(Moeda moeda) {
        return new Dinheiro(BigDecimal.ZERO, moeda);
    }

    public static Dinheiro zeroReais() {
        return zero(Moeda.BRL);
    }

    public Dinheiro somar(Dinheiro outro) {
        exigirMesmaMoeda(outro);
        return new Dinheiro(valor.add(outro.valor), moeda);
    }

    /**
     * Subtracao que resultaria em negativo e recusada. Ver decisao 2 no cabecalho.
     */
    public Dinheiro subtrair(Dinheiro outro) {
        exigirMesmaMoeda(outro);
        BigDecimal resultado = valor.subtract(outro.valor);
        if (resultado.signum() < 0) {
            throw new ValorMonetarioInvalido(
                    "subtrair %s de %s resultaria em valor negativo".formatted(outro, this));
        }
        return new Dinheiro(resultado, moeda);
    }

    public Dinheiro multiplicar(Quantidade quantidade) {
        return new Dinheiro(valor.multiply(BigDecimal.valueOf(quantidade.valor())), moeda);
    }

    /**
     * Aplica um percentual e arredonda uma unica vez, no resultado. O valor devolvido ja
     * e dinheiro de verdade, com duas casas.
     */
    public Dinheiro percentual(Percentual percentual) {
        return new Dinheiro(valor.multiply(percentual.comoFracao()), moeda);
    }

    /**
     * Menor entre os dois. Serve para limitar desconto ao valor do pedido sem precisar
     * de {@code if} espalhado por quem calcula.
     */
    public Dinheiro limitadoA(Dinheiro teto) {
        exigirMesmaMoeda(teto);
        return this.compareTo(teto) > 0 ? teto : this;
    }

    public boolean maiorQue(Dinheiro outro) {
        exigirMesmaMoeda(outro);
        return compareTo(outro) > 0;
    }

    public boolean menorQue(Dinheiro outro) {
        exigirMesmaMoeda(outro);
        return compareTo(outro) < 0;
    }

    public boolean ehZero() {
        return valor.signum() == 0;
    }

    @Override
    public int compareTo(Dinheiro outro) {
        exigirMesmaMoeda(outro);
        return valor.compareTo(outro.valor);
    }

    private void exigirMesmaMoeda(Dinheiro outro) {
        Objects.requireNonNull(outro, "operando e obrigatorio");
        if (moeda != outro.moeda) {
            throw new MoedasIncompativeis(moeda, outro.moeda);
        }
    }

    @Override
    public String toString() {
        return "%s %s".formatted(moeda, valor);
    }
}
