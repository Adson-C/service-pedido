package com.penambucanas.pedidos.fidelidade;

import com.penambucanas.pedidos.domain.TipoCliente;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * O indice unico em {@code pedidoId} e a rede de seguranca da idempotencia:
 * mesmo que o evento chegue duas vezes, o segundo insert falha.
 */
@Document(collection = "extrato_pontos")
public class LancamentoDePontos {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long pedidoId;

    private String eventoId;

    @Indexed
    private Long clienteId;

    private TipoCliente tipoCliente;

    private BigDecimal totalPago;

    private int pontos;

    private OffsetDateTime registradoEm;

    public LancamentoDePontos() {
    }

    /** Cria um novo lancamento de pontos a partir dos dados do evento "pedido-pago". */
    public LancamentoDePontos(Long pedidoId,
                              String eventoId,
                              Long clienteId,
                              TipoCliente tipoCliente,
                              BigDecimal totalPago,
                              int pontos,
                              OffsetDateTime registradoEm) {
        this.pedidoId = pedidoId;
        this.eventoId = eventoId;
        this.clienteId = clienteId;
        this.tipoCliente = tipoCliente;
        this.totalPago = totalPago;
        this.pontos = pontos;
        this.registradoEm = registradoEm;
    }

    /** @return o id do documento no MongoDB */
    public String getId() {
        return id;
    }

    /** @return o id do pedido que originou este lancamento (chave da idempotencia) */
    public Long getPedidoId() {
        return pedidoId;
    }

    /** @return o id do evento de mensageria que originou este lancamento */
    public String getEventoId() {
        return eventoId;
    }

    /** @return o id do cliente que recebeu os pontos */
    public Long getClienteId() {
        return clienteId;
    }

    /** @return o tipo do cliente no momento do lancamento (COMUM ou PLUS) */
    public TipoCliente getTipoCliente() {
        return tipoCliente;
    }

    /** @return o valor total pago no pedido que gerou os pontos */
    public BigDecimal getTotalPago() {
        return totalPago;
    }

    /** @return a quantidade de pontos creditados neste lancamento */
    public int getPontos() {
        return pontos;
    }

    /** @return a data/hora em que o credito foi registrado */
    public OffsetDateTime getRegistradoEm() {
        return registradoEm;
    }
}
