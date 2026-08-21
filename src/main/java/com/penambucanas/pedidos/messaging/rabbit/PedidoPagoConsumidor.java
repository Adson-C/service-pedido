package com.penambucanas.pedidos.messaging.rabbit;


import com.penambucanas.pedidos.fidelidade.FidelidadeService;
import com.penambucanas.pedidos.messaging.PedidoPagoEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor do evento "pedido-pago". Sua unica responsabilidade e tirar a
 * mensagem da fila e delegar para o {@link FidelidadeService} - a regra de
 */
@Component
public class PedidoPagoConsumidor {

    private static final Logger log = LoggerFactory.getLogger(PedidoPagoConsumidor.class);

    private final FidelidadeService fidelidadeService;

    public PedidoPagoConsumidor(FidelidadeService fidelidadeService) {
        this.fidelidadeService = fidelidadeService;
    }

    /** Escuta a fila de pedido-pago e delega o credito de pontos ao servico de fidelidade. */
    @RabbitListener(queues = RabbitMqConfig.FILA_PEDIDO_PAGO)
    public void consumir(PedidoPagoEvento evento) {
        log.info("Evento pedido-pago recebido. eventoId={} pedidoId={}",
                evento.eventoId(), evento.pedidoId());
        fidelidadeService.creditarPontosDoPedidoPago(evento);
    }
}
