package com.penambucanas.pedidos.domain;

import com.penambucanas.pedidos.exception.TransicaoDeStatusInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PedidoStatusTest {

    private static final Produto CAMISETA =
            new Produto(1L, "Camiseta", "VESTUARIO", new BigDecimal("50.00"), 10);

    /** Monta um pedido novo (status CRIADO) com 1 camiseta, ponto de partida da maioria dos testes abaixo. */
    private Pedido pedidoNovo() {
        Pedido pedido = new Pedido(1L, new Cliente(1L, "Giselle", TipoCliente.COMUM), null);
        pedido.adicionarItem(new ItemPedido(CAMISETA, 1));
        return pedido;
    }

    /** Todo pedido comeca no status CRIADO. */
    @Test
    @DisplayName("Pedido nasce em CRIADO")
    void pedidoNasceCriado() {
        assertThat(pedidoNovo().getStatus()).isEqualTo(StatusPedido.CRIADO);
    }

    @Test
    @DisplayName("Fluxo feliz: CRIADO -> PAGO -> ENVIADO -> ENTREGUE")
    void fluxoFeliz() {
        Pedido pedido = pedidoNovo();

        pedido.pagar(100);
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.PAGO);
        assertThat(pedido.getPontosGerados()).isEqualTo(100);

        pedido.enviar();
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.ENVIADO);

        pedido.entregar();
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.ENTREGUE);
    }

    @Test
    @DisplayName("So e possivel pagar um pedido CRIADO")
    void naoPagaPedidoJaPago() {
        Pedido pedido = pedidoNovo();
        pedido.pagar(100);

        assertThatThrownBy(() -> pedido.pagar(100))
                .isInstanceOf(TransicaoDeStatusInvalidaException.class)
                .hasMessageContaining("PAGO");
    }

    @Test
    @DisplayName("Pedido CRIADO pode ser cancelado")
    void cancelaPedidoCriado() {
        Pedido pedido = pedidoNovo();

        pedido.cancelar();

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CANCELADO);
    }

    @Test
    @DisplayName("Pedido PAGO pode ser cancelado")
    void cancelaPedidoPago() {
        Pedido pedido = pedidoNovo();
        pedido.pagar(100);

        pedido.cancelar();

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CANCELADO);
    }

    @Test
    @DisplayName("Pedido ENVIADO nao pode ser cancelado")
    void naoCancelaPedidoEnviado() {
        Pedido pedido = pedidoNovo();
        pedido.pagar(100);
        pedido.enviar();

        assertThatThrownBy(pedido::cancelar)
                .isInstanceOf(TransicaoDeStatusInvalidaException.class);
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.ENVIADO);
    }

    @Test
    @DisplayName("Pedido ENTREGUE nao pode ser cancelado")
    void naoCancelaPedidoEntregue() {
        Pedido pedido = pedidoNovo();
        pedido.pagar(100);
        pedido.enviar();
        pedido.entregar();

        assertThatThrownBy(pedido::cancelar)
                .isInstanceOf(TransicaoDeStatusInvalidaException.class);
    }

    @Test
    @DisplayName("Pedido CANCELADO nao pode ser pago")
    void naoPagaPedidoCancelado() {
        Pedido pedido = pedidoNovo();
        pedido.cancelar();

        assertThatThrownBy(() -> pedido.pagar(100))
                .isInstanceOf(TransicaoDeStatusInvalidaException.class);
    }

    @Test
    @DisplayName("Nao e possivel enviar um pedido que ainda nao foi pago")
    void naoEnviaPedidoNaoPago() {
        assertThatThrownBy(() -> pedidoNovo().enviar())
                .isInstanceOf(TransicaoDeStatusInvalidaException.class);
    }

    /** {@code isFinal()} deve ser verdadeiro apenas para ENTREGUE e CANCELADO, e falso para um status intermediario como CRIADO. */
    @Test
    @DisplayName("ENTREGUE e CANCELADO sao estados finais")
    void estadosFinais() {
        assertThat(StatusPedido.ENTREGUE.isFinal()).isTrue();
        assertThat(StatusPedido.CANCELADO.isFinal()).isTrue();
        assertThat(StatusPedido.CRIADO.isFinal()).isFalse();
    }
}
