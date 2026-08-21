package com.penambucanas.pedidos.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum StatusPedido{

    CRIADO,
    PAGO,
    ENVIADO,
    ENTREGUE,
    CANCELADO;

    private static final Map<StatusPedido, Set<StatusPedido>> TRANSICOES_PERMITIDAS =
            new EnumMap<>(StatusPedido.class);

    static {
        TRANSICOES_PERMITIDAS.put(CRIADO, EnumSet.of(PAGO, CANCELADO));
        TRANSICOES_PERMITIDAS.put(PAGO, EnumSet.of(ENVIADO, CANCELADO));
        TRANSICOES_PERMITIDAS.put(ENVIADO, EnumSet.of(ENTREGUE));
        TRANSICOES_PERMITIDAS.put(ENTREGUE, EnumSet.noneOf(StatusPedido.class));
        TRANSICOES_PERMITIDAS.put(CANCELADO, EnumSet.noneOf(StatusPedido.class));
    }

    /** @return {@code true} se, a partir deste status, e permitido ir para {@code destino} */
    public boolean podeTransitarPara(StatusPedido destino) {
        return getTransicoesPermitidas().contains(destino);
    }

    /** @return o conjunto (imutavel) de status para os quais este status pode transitar */
    public Set<StatusPedido> getTransicoesPermitidas() {
        return Collections.unmodifiableSet(TRANSICOES_PERMITIDAS.get(this));
    }

    /** @return {@code true} se este status nao permite mais nenhuma transicao (ENTREGUE ou CANCELADO) */
    public boolean isFinal() {
        return getTransicoesPermitidas().isEmpty();
    }
}
