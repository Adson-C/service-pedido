package com.penambucanas.pedidos.domain.regras;

import java.math.BigDecimal;

public record ResumoFinanceiro(BigDecimal subtotal,
                               BigDecimal desconto,
                               BigDecimal frete,
                               BigDecimal total) {

    public ResumoFinanceiro {
        subtotal = RegrasDoPedido.emReais(subtotal);
        desconto = RegrasDoPedido.emReais(desconto);
        frete = RegrasDoPedido.emReais(frete);
        total = RegrasDoPedido.emReais(total);
    }
}
