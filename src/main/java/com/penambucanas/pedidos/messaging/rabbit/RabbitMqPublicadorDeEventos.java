package com.penambucanas.pedidos.messaging.rabbit;


import com.penambucanas.pedidos.messaging.PedidoPagoEvento;
import com.penambucanas.pedidos.messaging.PublicadorDeEventos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Implementacao de {@link PublicadorDeEventos} sobre RabbitMQ.
 *
 */
@Component
public class RabbitMqPublicadorDeEventos implements PublicadorDeEventos {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqPublicadorDeEventos.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitMqPublicadorDeEventos(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }
    /** Publica o evento na exchange de pedidos, com a routing key "pedido.pago", serializado em JSON. */
    @Override
    public void publicarPedidoPago(PedidoPagoEvento evento) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.EXCHANGE_PEDIDOS,
                RabbitMqConfig.ROUTING_KEY_PEDIDO_PAGO,
                evento);
        log.info("Evento pedido-pago publicado. eventoId={} pedidoId={}",
                evento.eventoId(), evento.pedidoId());
    }
}
