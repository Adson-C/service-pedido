package com.penambucanas.pedidos.api;


import com.penambucanas.pedidos.exception.RecursoNaoEncontradoException;
import com.penambucanas.pedidos.exception.RegraDeNegocioException;
import com.penambucanas.pedidos.exception.TransicaoDeStatusInvalidaException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** Recurso inexistente */
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> tratarNaoEncontrado(RecursoNaoEncontradoException e,
                                                            HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, "Recurso nao encontrado", e.getMessage(), request);
    }

    /** Transicao de status invalida: o recurso existe, mas nao neste estado. */
    @ExceptionHandler(TransicaoDeStatusInvalidaException.class)
    public ResponseEntity<ErroResponse> tratarTransicaoInvalida(TransicaoDeStatusInvalidaException e,
                                                                HttpServletRequest request) {
        return responder(HttpStatus.CONFLICT, "Transicao de status invalida", e.getMessage(), request);
    }

    /**
     * Demais violacoes de regra de negocio (estoque insuficiente, cupom
     * invalido, pedido sem itens): 422, como pede o caso D.
     */
    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResponse> tratarRegraDeNegocio(RegraDeNegocioException e,
                                                             HttpServletRequest request) {
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negocio violada", e.getMessage(), request);
    }

    /** Payload malformado segundo o Bean Validation */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException e,
                                                        HttpServletRequest request) {
        List<ErroResponse.CampoInvalido> campos = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ErroResponse.CampoInvalido(erro.getField(), erro.getDefaultMessage()))
                .toList();

        ErroResponse corpo = ErroResponse.deValidacao(
                HttpStatus.BAD_REQUEST.value(),
                "Requisicao invalida",
                "Ha campos invalidos na requisicao.",
                request.getRequestURI(),
                campos);

        return ResponseEntity.badRequest().body(corpo);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarErroInesperado(Exception e, HttpServletRequest request) {
        log.error("Erro inesperado ao processar {}", request.getRequestURI(), e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado ao processar a requisicao.", request);
    }

    /** Monta o corpo padrao de erro ({@link ErroResponse}) para o status HTTP informado. */
    private ResponseEntity<ErroResponse> responder(HttpStatus status,
                                                   String erro,
                                                   String mensagem,
                                                   HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ErroResponse.de(status.value(), erro, mensagem, request.getRequestURI()));
    }
}
