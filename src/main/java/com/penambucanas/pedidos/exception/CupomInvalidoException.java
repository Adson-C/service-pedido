package com.penambucanas.pedidos.exception;

public class CupomInvalidoException extends RegraDeNegocioException {

    /** @param codigoInformado o codigo de cupom que nao corresponde a nenhum cupom valido */
    public CupomInvalidoException(String codigoInformado) {
        super("Cupom invalido: '" + codigoInformado + "'. Cupons aceitos: DESC10, FRETEGRATIS.");
    }
}
