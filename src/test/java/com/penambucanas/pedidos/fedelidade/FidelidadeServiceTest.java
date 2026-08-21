package com.penambucanas.pedidos.fedelidade;

import com.penambucanas.pedidos.domain.TipoCliente;
import com.penambucanas.pedidos.fidelidade.ExtratoDePontos;
import com.penambucanas.pedidos.fidelidade.ExtratoDePontosRepository;
import com.penambucanas.pedidos.fidelidade.FidelidadeService;
import com.penambucanas.pedidos.fidelidade.LancamentoDePontos;
import com.penambucanas.pedidos.messaging.PedidoPagoEvento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FidelidadeServiceTest {

    @Mock
    private ExtratoDePontosRepository extratoRepository;

    @InjectMocks
    private FidelidadeService fidelidadeService;

    @Captor
    private ArgumentCaptor<LancamentoDePontos> lancamentoCaptor;

    /** Monta um evento "pedido-pago" de teste (pedido 10, cliente PLUS, R$ 285,00, 570 pontos). */
    private PedidoPagoEvento evento(String eventoId) {
        return new PedidoPagoEvento(
                eventoId, 10L, 1L, TipoCliente.PLUS,
                new BigDecimal("285.00"), 570, OffsetDateTime.now());
    }

    @Test
    @DisplayName("Credita os pontos no extrato quando o evento chega pela primeira vez")
    void creditaPontosNaPrimeiraEntrega() {
        when(extratoRepository.existsByPedidoId(10L)).thenReturn(false);

        fidelidadeService.creditarPontosDoPedidoPago(evento("evento-1"));

        verify(extratoRepository).save(lancamentoCaptor.capture());
        LancamentoDePontos lancamento = lancamentoCaptor.getValue();
        assertThat(lancamento.getPedidoId()).isEqualTo(10L);
        assertThat(lancamento.getClienteId()).isEqualTo(1L);
        assertThat(lancamento.getPontos()).isEqualTo(570);
        assertThat(lancamento.getTotalPago()).isEqualByComparingTo("285.00");
        assertThat(lancamento.getEventoId()).isEqualTo("evento-1");
    }

    @Test
    @DisplayName("Evento duplicado do mesmo pedido nao credita pontos de novo")
    void eventoDuplicadoNaoCreditaDeNovo() {
        when(extratoRepository.existsByPedidoId(10L)).thenReturn(true);

        fidelidadeService.creditarPontosDoPedidoPago(evento("evento-1"));

        verify(extratoRepository, never()).save(any(LancamentoDePontos.class));
    }

    @Test
    @DisplayName("O extrato soma os pontos dos lancamentos do cliente")
    void extratoSomaPontos() {
        when(extratoRepository.findByClienteIdOrderByRegistradoEmDesc(1L)).thenReturn(List.of(
                new LancamentoDePontos(10L, "e1", 1L, TipoCliente.PLUS,
                        new BigDecimal("285.00"), 570, OffsetDateTime.now()),
                new LancamentoDePontos(11L, "e2", 1L, TipoCliente.PLUS,
                        new BigDecimal("110.00"), 220, OffsetDateTime.now())));

        ExtratoDePontos extrato = fidelidadeService.consultarExtrato(1L);

        assertThat(extrato.clienteId()).isEqualTo(1L);
        assertThat(extrato.saldoPontos()).isEqualTo(790);
        assertThat(extrato.lancamentos()).hasSize(2);
    }

    @Test
    @DisplayName("Cliente sem lancamentos tem saldo zero")
    void clienteSemLancamentos() {
        when(extratoRepository.findByClienteIdOrderByRegistradoEmDesc(99L)).thenReturn(List.of());

        ExtratoDePontos extrato = fidelidadeService.consultarExtrato(99L);

        assertThat(extrato.saldoPontos()).isZero();
        assertThat(extrato.lancamentos()).isEmpty();
    }

}
