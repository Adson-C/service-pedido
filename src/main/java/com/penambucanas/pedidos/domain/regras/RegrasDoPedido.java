package com.penambucanas.pedidos.domain.regras;

import java.math.BigDecimal;
import java.math.RoundingMode;


public final class RegrasDoPedido  {

    /** desconto de 5% para cliente PLUS. */
    public static final BigDecimal PERCENTUAL_DESCONTO_PLUS = new BigDecimal("0.05");

    /** desconto de 10% do cupom DESC10. */
    public static final BigDecimal PERCENTUAL_DESCONTO_CUPOM_DESC10 = new BigDecimal("0.10");

    /** frete fixo de R$ 20,00. */
    public static final BigDecimal VALOR_FRETE_PADRAO = new BigDecimal("20.00");

    /** frete gratis a partir de R$ 200,00 de subtotal. */
    public static final BigDecimal SUBTOTAL_MINIMO_FRETE_GRATIS = new BigDecimal("200.00");

    /** valor do frete quando ele e gratuito. */
    public static final BigDecimal FRETE_GRATIS = BigDecimal.ZERO;

    /** 1 ponto para cada R$ 1,00 do total pago. */
    public static final BigDecimal REAIS_POR_PONTO = BigDecimal.ONE;

    /** cliente PLUS acumula o dobro de pontos. */
    public static final int MULTIPLICADOR_PONTOS_PLUS = 2;

    /** Todos os valores monetarios usam duas casas decimais (secao 2.2). */
    public static final int CASAS_DECIMAIS_MONETARIAS = 2;

    public static final RoundingMode ARREDONDAMENTO_MONETARIO = RoundingMode.HALF_UP;

    private RegrasDoPedido() {
    }

    /** Normaliza um valor monetario para duas casas decimais. */
    public static BigDecimal emReais(BigDecimal valor) {
        return valor.setScale(CASAS_DECIMAIS_MONETARIAS, ARREDONDAMENTO_MONETARIO);
    }
}
