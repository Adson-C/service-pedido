package com.penambucanas.pedidos.exception;

public class EstoqueInsuficienteException extends RegraDeNegocioException {

    public EstoqueInsuficienteException(String mensagem) {
        super(mensagem);
    }

    /** Monta a excecao com uma mensagem padronizada, citando produto, quantidades pedida e disponivel. */
    public static EstoqueInsuficienteException para(String nomeProduto,
                                                    Long produtoId,
                                                    int quantidadeSolicitada,
                                                    int estoqueDisponivel) {
        return new EstoqueInsuficienteException(
                "Estoque insuficiente para o produto '" + nomeProduto + "' (id " + produtoId
                        + "): solicitado " + quantidadeSolicitada
                        + ", disponivel " + estoqueDisponivel + ".");
    }
}
