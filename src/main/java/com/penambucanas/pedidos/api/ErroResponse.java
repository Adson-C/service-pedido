package com.penambucanas.pedidos.api;

import java.time.OffsetDateTime;
import java.util.List;

public record ErroResponse(OffsetDateTime timestamp,
                           int status,
                           String erro,
                           String mensagem,
                           String caminho,
                           List<CampoInvalido> camposInvalidos) {


    public record CampoInvalido(String campo, String mensagem) {
    }

    /** Cria um erro simples (sem lista de campos invalidos), com o timestamp atual. */
    public static ErroResponse de(int status, String erro, String mensagem, String caminho) {
        return new ErroResponse(OffsetDateTime.now(), status, erro, mensagem, caminho, List.of());
    }

    /** Cria um erro de validacao de Bean Validation, com a lista de campos que falharam. */
    public static ErroResponse deValidacao(int status,
                                           String erro,
                                           String mensagem,
                                           String caminho,
                                           List<CampoInvalido> camposInvalidos) {
        return new ErroResponse(OffsetDateTime.now(), status, erro, mensagem, caminho, camposInvalidos);
    }
}
