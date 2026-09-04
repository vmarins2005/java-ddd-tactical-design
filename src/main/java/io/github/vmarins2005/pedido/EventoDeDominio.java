package io.github.vmarins2005.pedido;

import java.time.LocalDateTime;

/**
 * Evento de dominio: um fato que ja aconteceu.
 *
 * <p>Repare nos nomes - {@code PedidoPago}, {@code PedidoCancelado} - todos no particípio.
 * Evento nomeado no imperativo ({@code EnviarEmail}) e comando disfarcado: acopla o
 * agregado a decisao de quem vai reagir, que e exatamente o que o evento deveria
 * desacoplar.
 *
 * <p>Os eventos sao acumulados dentro do agregado e lidos por quem o salva. Publicar
 * daqui exigiria o dominio conhecer um publicador - e ai a garantia de "salvou, entao
 * publicou" nao existiria. Essa garantia e o assunto do projeto de outbox da serie.
 */
public sealed interface EventoDeDominio
        permits PedidoFechado, PagamentoRegistrado, PedidoPago, PedidoCancelado {

    PedidoId pedidoId();

    LocalDateTime ocorridoEm();
}
