package com.penambucanas.pedidos.messaging;

import com.penambucanas.pedidos.domain.Pedido;
import com.penambucanas.pedidos.domain.TipoCliente;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * sistemas precisam. O {@code eventoId} permite ao consumidor detectar entregas
 * duplicadas.
 */
public record PedidoPagoEvento(String eventoId,
                               Long pedidoId,
                               Long clienteId,
                               TipoCliente tipoCliente,
                               BigDecimal totalPago,
                               int pontosGerados,
                               OffsetDateTime ocorridoEm) {

    /** Monta o evento a partir de um pedido ja pago, gerando um {@code eventoId} novo. */
    public static PedidoPagoEvento de(Pedido pedido) {
        return new PedidoPagoEvento(
                UUID.randomUUID().toString(),
                pedido.getId(),
                pedido.getCliente().getId(),
                pedido.getCliente().getTipo(),
                pedido.getTotal(),
                pedido.getPontosGerados(),
                OffsetDateTime.now());
    }
}
