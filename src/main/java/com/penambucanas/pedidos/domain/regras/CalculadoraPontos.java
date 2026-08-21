package com.penambucanas.pedidos.domain.regras;

import com.penambucanas.pedidos.domain.TipoCliente;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Assim como a {@link CalculadoraPedido}, e uma regra pura e testavel em unidade.
 */
@Component
public class CalculadoraPontos {

    public int calcular(BigDecimal totalPago, TipoCliente tipoCliente) {
        if (totalPago == null || totalPago.signum() <= 0) {
            return 0;
        }
        int pontosBase = totalPago
                .divide(RegrasDoPedido.REAIS_POR_PONTO, 0, RoundingMode.DOWN)
                .intValue();

        return (tipoCliente != null && tipoCliente.isPlus())
                ? pontosBase * RegrasDoPedido.MULTIPLICADOR_PONTOS_PLUS
                : pontosBase;
    }
}
