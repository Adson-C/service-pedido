package com.penambucanas.pedidos.domain.regras;

import com.penambucanas.pedidos.domain.Cupom;
import com.penambucanas.pedidos.domain.ItemPedido;
import com.penambucanas.pedidos.domain.TipoCliente;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * A classe nao conhece banco, HTTP nem mensageria - recebe dados e devolve um
 * {@link ResumoFinanceiro}. Por isso da para testar cada regra isoladamente,
 * sem subir contexto do Spring.
 */
@Component
public class CalculadoraPedido {

    /**
     * @param itens        itens ja com preco unitario definido
     * @param tipoCliente  COMUM ou PLUS
     * @param cupom        cupom aplicado, ou {@code null} se nao houver
     */
    public ResumoFinanceiro calcular(List<ItemPedido> itens, TipoCliente tipoCliente, Cupom cupom) {
        BigDecimal subtotal = calcularSubtotal(itens);
        BigDecimal desconto = calcularDesconto(subtotal, tipoCliente, cupom);
        BigDecimal frete = calcularFrete(subtotal, cupom);
        BigDecimal total = calcularTotal(subtotal, desconto, frete);
        return new ResumoFinanceiro(subtotal, desconto, frete, total);
    }

    /** subtotal = soma de (precoUnitario x quantidade). */
    private BigDecimal calcularSubtotal(List<ItemPedido> itens) {
        return itens.stream()
                .map(ItemPedido::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** desconto percentual = (5% se PLUS) + (10% se DESC10), sobre o subtotal. */
    private BigDecimal calcularDesconto(BigDecimal subtotal, TipoCliente tipoCliente, Cupom cupom) {
        BigDecimal percentual = BigDecimal.ZERO;
        if (tipoCliente != null && tipoCliente.isPlus()) {
            percentual = percentual.add(RegrasDoPedido.PERCENTUAL_DESCONTO_PLUS);
        }
        if (cupom != null) {
            percentual = percentual.add(cupom.getPercentualDesconto());
        }
        return RegrasDoPedido.emReais(subtotal.multiply(percentual));
    }

    /** frete fixo, gratis se subtotal >= R$ 200,00 ou se o cupom isentar. */
    private BigDecimal calcularFrete(BigDecimal subtotal, Cupom cupom) {
        boolean atingiuLimiteFreteGratis =
                subtotal.compareTo(RegrasDoPedido.SUBTOTAL_MINIMO_FRETE_GRATIS) >= 0;
        boolean cupomIsentaFrete = cupom != null && cupom.isentaFrete();

        return (atingiuLimiteFreteGratis || cupomIsentaFrete)
                ? RegrasDoPedido.FRETE_GRATIS
                : RegrasDoPedido.VALOR_FRETE_PADRAO;
    }

    /** total = subtotal - desconto + frete, nunca negativo. */
    private BigDecimal calcularTotal(BigDecimal subtotal, BigDecimal desconto, BigDecimal frete) {
        BigDecimal total = subtotal.subtract(desconto).add(frete);
        return total.max(BigDecimal.ZERO);
    }
}
