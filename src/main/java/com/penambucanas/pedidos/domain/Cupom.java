package com.penambucanas.pedidos.domain;

import com.penambucanas.pedidos.domain.regras.RegrasDoPedido;
import com.penambucanas.pedidos.exception.CupomInvalidoException;

import java.math.BigDecimal;
import java.util.Arrays;

public enum Cupom {

    /** 10% de desconto sobre o subtotal. */
    DESC10(RegrasDoPedido.PERCENTUAL_DESCONTO_CUPOM_DESC10, false),

    /** zera o frete, independente do subtotal. */
    FRETEGRATIS(BigDecimal.ZERO, true);

    private final BigDecimal percentualDesconto;
    private final boolean isentaFrete;

    Cupom(BigDecimal percentualDesconto, boolean isentaFrete) {
        this.percentualDesconto = percentualDesconto;
        this.isentaFrete = isentaFrete;
    }

    public BigDecimal getPercentualDesconto() {
        return percentualDesconto;
    }

    public boolean isentaFrete() {
        return isentaFrete;
    }

    /**
     * Converte o codigo informado na requisicao em um cupom.
     *
     * @param codigo codigo do cupom; {@code null} ou em branco significa "sem cupom"
     * @return o cupom correspondente ou {@code null} quando nao ha cupom
     * @throws CupomInvalidoException se o codigo nao corresponder a nenhum cupom
     */
    public static Cupom deCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        String normalizado = codigo.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(cupom -> cupom.name().equals(normalizado))
                .findFirst()
                .orElseThrow(() -> new CupomInvalidoException(codigo));
    }
}
