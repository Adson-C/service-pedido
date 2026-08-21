package com.penambucanas.pedidos.fidelidade;

import com.penambucanas.pedidos.messaging.PedidoPagoEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Credita os pontos de fidelidade a partir do evento "pedido-pago".
 * mensagem mais de uma vez. O credito e chaveado pelo {@code pedidoId}, com
 */
@Service
public class FidelidadeService {

    private static final Logger log = LoggerFactory.getLogger(FidelidadeService.class);

    private final ExtratoDePontosRepository extratoRepository;

    public FidelidadeService(ExtratoDePontosRepository extratoRepository) {
        this.extratoRepository = extratoRepository;
    }

    /**
     * Credita os pontos do evento "pedido-pago" no extrato do cliente.
     * Idempotente: se ja existir um lancamento para o {@code pedidoId}, apenas loga e nao faz nada.
     *
     * @param evento evento recebido do RabbitMQ (ou publicado diretamente em testes)
     */
    public void creditarPontosDoPedidoPago(PedidoPagoEvento evento) {
        if (extratoRepository.existsByPedidoId(evento.pedidoId())) {
            log.info("Evento pedido-pago ignorado (pontos ja creditados). pedidoId={} eventoId={}",
                    evento.pedidoId(), evento.eventoId());
            return;
        }

        LancamentoDePontos lancamento = new LancamentoDePontos(
                evento.pedidoId(),
                evento.eventoId(),
                evento.clienteId(),
                evento.tipoCliente(),
                evento.totalPago(),
                evento.pontosGerados(),
                OffsetDateTime.now());

        try {
            extratoRepository.save(lancamento);
            log.info("Pontos creditados. clienteId={} pedidoId={} pontos={}",
                    evento.clienteId(), evento.pedidoId(), evento.pontosGerados());
        } catch (DuplicateKeyException e) {
            // Duas entregas concorrentes do mesmo evento: o indice unico garante o resto.
            log.info("Credito duplicado bloqueado pelo indice unico. pedidoId={}", evento.pedidoId());
        }
    }

    /**
     * Consulta o extrato de pontos de um cliente.
     *
     * @param clienteId id do cliente
     * @return extrato com o saldo somado e todos os lancamentos, do mais recente para o mais antigo
     */
    public ExtratoDePontos consultarExtrato(Long clienteId) {
        List<LancamentoDePontos> lancamentos =
                extratoRepository.findByClienteIdOrderByRegistradoEmDesc(clienteId);
        return ExtratoDePontos.de(clienteId, lancamentos);
    }
}
