package com.penambucanas.pedidos.domain;

public enum TipoCliente {
    COMUM,
    PLUS;
    /** @return {@code true} se este tipo for PLUS */
    public boolean isPlus() {
        return this == PLUS;
    }
}
