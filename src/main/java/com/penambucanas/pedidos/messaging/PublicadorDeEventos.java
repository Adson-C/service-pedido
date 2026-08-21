package com.penambucanas.pedidos.messaging;

/**
 * O {@code PedidoService} depende desta interface, nunca do RabbitMQ. Trocar o
 * broker por Kafka/Pub-Sub, ou por uma implementacao em memoria nos testes,
 * significa trocar a implementacao - a regra de negocio nao muda.
 */
public interface PublicadorDeEventos {

    /** Publica o evento de pedido pago para quem estiver interessado (hoje, o modulo de fidelidade). */
    void publicarPedidoPago(PedidoPagoEvento evento);
}
