package com.penambucanas.pedidos.messaging.rabbit;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE_PEDIDOS = "pedidos.exchange";
    public static final String ROUTING_KEY_PEDIDO_PAGO = "pedido.pago";
    public static final String FILA_PEDIDO_PAGO = "pedidos.pedido-pago";

    public static final String EXCHANGE_DLQ = "pedidos.dlx";
    public static final String FILA_PEDIDO_PAGO_DLQ = "pedidos.pedido-pago.dlq";

    @Bean
    TopicExchange pedidosExchange() {
        return new TopicExchange(EXCHANGE_PEDIDOS, true, false);
    }


    @Bean
    TopicExchange pedidosDeadLetterExchange() {
        return new TopicExchange(EXCHANGE_DLQ, true, false);
    }

    @Bean
    Queue filaPedidoPago() {
        return QueueBuilder.durable(FILA_PEDIDO_PAGO)
                .deadLetterExchange(EXCHANGE_DLQ)
                .deadLetterRoutingKey(ROUTING_KEY_PEDIDO_PAGO)
                .build();
    }

    @Bean
    Queue filaPedidoPagoDlq() {
        return QueueBuilder.durable(FILA_PEDIDO_PAGO_DLQ).build();
    }

    /** Liga a fila principal a exchange de pedidos pela routing key "pedido.pago". */
    @Bean
    Binding bindingPedidoPago(Queue filaPedidoPago, TopicExchange pedidosExchange) {
        return BindingBuilder.bind(filaPedidoPago).to(pedidosExchange).with(ROUTING_KEY_PEDIDO_PAGO);
    }

    /** Liga a fila morta a exchange de dead-letter. */
    @Bean
    Binding bindingPedidoPagoDlq(Queue filaPedidoPagoDlq, TopicExchange pedidosDeadLetterExchange) {
        return BindingBuilder.bind(filaPedidoPagoDlq).to(pedidosDeadLetterExchange).with(ROUTING_KEY_PEDIDO_PAGO);
    }

    /** Mensagens trafegam em JSON, e nao em serializacao binaria de Java. */
    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
