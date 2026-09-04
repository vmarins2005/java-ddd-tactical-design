package io.github.vmarins2005.pedido;

import java.time.LocalDateTime;

public class PrazoDeCancelamentoExpirado extends RegraDeNegocioViolada {

    public PrazoDeCancelamentoExpirado(LocalDateTime pagoEm, int diasDePrazo) {
        super("pedido pago em %s so podia ser cancelado em ate %d dias".formatted(pagoEm, diasDePrazo));
    }
}
