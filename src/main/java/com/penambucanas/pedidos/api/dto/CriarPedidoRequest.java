package com.penambucanas.pedidos.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CriarPedidoRequest(

        @NotNull(message = "clienteId e obrigatorio")
        Long clienteId,

        @NotEmpty(message = "O pedido precisa ter pelo menos 1 item")
        @Valid
        List<ItemRequest> itens,

        String cupom) {

    /** Uma linha do pedido: qual produto e em qual quantidade. */
    public record ItemRequest(

            @NotNull(message = "produtoId e obrigatorio")
            Long produtoId,

            @Positive(message = "A quantidade deve ser maior que zero")
            int quantidade) {
    }
}
