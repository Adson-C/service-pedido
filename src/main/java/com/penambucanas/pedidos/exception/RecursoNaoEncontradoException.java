package com.penambucanas.pedidos.exception;

public class RecursoNaoEncontradoException extends RuntimeException {

    /**
     * @param recurso nome do tipo de recurso (ex.: "Cliente", "Produto", "Pedido")
     * @param id      id que foi buscado e nao encontrado
     */
    public RecursoNaoEncontradoException(String recurso, Object id) {
        super(recurso + " nao encontrado(a) para o id " + id + ".");
    }
}
