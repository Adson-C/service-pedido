package com.penambucanas.pedidos.api.dto;

import com.penambucanas.pedidos.domain.Produto;

import java.math.BigDecimal;

/** Representacao do catalogo (GET /produtos). */
public record ProdutoResponse(Long id,
                              String nome,
                              String categoria,
                              BigDecimal preco,
                              int estoque) {

    /** Converte a entidade {@link Produto} no seu DTO de resposta. */
    public static ProdutoResponse de(Produto produto) {
        return new ProdutoResponse(
                produto.getId(),
                produto.getNome(),
                produto.getCategoria(),
                produto.getPreco(),
                produto.getEstoque());
    }
}
