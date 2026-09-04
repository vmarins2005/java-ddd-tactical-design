package io.github.vmarins2005.pedido;

import java.time.LocalDateTime;
import java.util.Objects;

public record Pagamento(String referencia, Dinheiro valor, LocalDateTime recebidoEm) {

    public Pagamento {
        Objects.requireNonNull(referencia, "referencia do pagamento e obrigatoria");
        Objects.requireNonNull(valor, "valor do pagamento e obrigatorio");
        Objects.requireNonNull(recebidoEm, "data do pagamento e obrigatoria");
        if (valor.ehZero()) {
            throw new ValorMonetarioInvalido("pagamento de valor zero nao e pagamento");
        }
    }
}
