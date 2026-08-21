package com.penambucanas.pedidos.api.dto;

import com.penambucanas.pedidos.domain.Cupom;
import com.penambucanas.pedidos.domain.ItemPedido;
import com.penambucanas.pedidos.domain.Pedido;
import com.penambucanas.pedidos.domain.StatusPedido;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record PedidoResponse(Long id,
                             Long clienteId,
                             String cupom,
                             StatusPedido status,
                             List<ItemResponse> itens,
                             BigDecimal subtotal,
                             BigDecimal desconto,
                             BigDecimal frete,
                             BigDecimal total,
                             int pontosGerados,
                             OffsetDateTime criadoEm) {

    /** Representacao de leitura de um item do pedido. */
    public record ItemResponse(Long produtoId,
                               String nomeProduto,
                               int quantidade,
                               BigDecimal precoUnitario,
                               BigDecimal valorTotal) {

        /** Converte a entidade {@link ItemPedido} no seu DTO de resposta. */
        static ItemResponse de(ItemPedido item) {
            return new ItemResponse(
                    item.getProduto().getId(),
                    item.getProduto().getNome(),
                    item.getQuantidade(),
                    item.getPrecoUnitario(),
                    item.getValorTotal());
        }
    }

    /** Converte a entidade {@link Pedido} no seu DTO de resposta. */
    public static PedidoResponse de(Pedido pedido) {
        Cupom cupom = pedido.getCupom();
        return new PedidoResponse(
                pedido.getId(),
                pedido.getCliente().getId(),
                cupom == null ? null : cupom.name(),
                pedido.getStatus(),
                pedido.getItens().stream().map(ItemResponse::de).toList(),
                pedido.getSubtotal(),
                pedido.getDesconto(),
                pedido.getFrete(),
                pedido.getTotal(),
                pedido.getPontosGerados(),
                pedido.getCriadoEm());
    }
}
