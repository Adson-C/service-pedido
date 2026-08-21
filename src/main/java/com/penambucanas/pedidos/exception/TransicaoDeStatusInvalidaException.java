package com.penambucanas.pedidos.exception;

import com.penambucanas.pedidos.domain.StatusPedido;

public class TransicaoDeStatusInvalidaException extends RegraDeNegocioException {

    /**
     * @param atual   status em que o pedido esta hoje
     * @param destino status para o qual se tentou transitar
     */
    public TransicaoDeStatusInvalidaException(StatusPedido atual, StatusPedido destino) {
        super("Transicao de status invalida: pedido em " + atual + " nao pode ir para " + destino
                + ". Transicoes permitidas a partir de " + atual + ": " + atual.getTransicoesPermitidas() + ".");
    }
}
