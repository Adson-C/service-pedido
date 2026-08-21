package com.penambucanas.pedidos.domain.regras;

import com.penambucanas.pedidos.domain.Cupom;
import com.penambucanas.pedidos.domain.ItemPedido;
import com.penambucanas.pedidos.domain.Produto;
import com.penambucanas.pedidos.domain.TipoCliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CalculadoraPedidoTest {

    private static final Produto CAMISETA = new Produto(1L, "Camiseta", "VESTUARIO", new BigDecimal("50.00"), 10);
    private static final Produto TENIS = new Produto(2L, "Tenis", "CALCADOS", new BigDecimal("80.00"), 5);
    private static final Produto FONE = new Produto(3L, "Fone", "ELETRONICOS", new BigDecimal("300.00"), 2);

    private final CalculadoraPedido calculadora = new CalculadoraPedido();

    @Nested
    @DisplayName("Casos de aceitacao da especificacao")
    class CasosDeAceitacao {

        @Test
        @DisplayName("Caso A - COMUM sem cupom: 2x Camiseta + 1x Tenis => 180 / 0 / 20 / 200")
        void casoA() {
            ResumoFinanceiro resumo = calculadora.calcular(
                    List.of(new ItemPedido(CAMISETA, 2), new ItemPedido(TENIS, 1)),
                    TipoCliente.COMUM,
                    null);

            assertThat(resumo.subtotal()).isEqualByComparingTo("180.00");
            assertThat(resumo.desconto()).isEqualByComparingTo("0.00");
            assertThat(resumo.frete()).isEqualByComparingTo("20.00");
            assertThat(resumo.total()).isEqualByComparingTo("200.00");
        }

        @Test
        @DisplayName("Caso B - PLUS com FRETEGRATIS: 1x Fone => 300 / 15 / 0 / 285")
        void casoB() {
            ResumoFinanceiro resumo = calculadora.calcular(
                    List.of(new ItemPedido(FONE, 1)),
                    TipoCliente.PLUS,
                    Cupom.FRETEGRATIS);

            assertThat(resumo.subtotal()).isEqualByComparingTo("300.00");
            assertThat(resumo.desconto()).isEqualByComparingTo("15.00");
            assertThat(resumo.frete()).isEqualByComparingTo("0.00");
            assertThat(resumo.total()).isEqualByComparingTo("285.00");
        }

        @Test
        @DisplayName("Caso C - COMUM com DESC10: 2x Camiseta em duas linhas => 100 / 10 / 20 / 110")
        void casoC() {
            ResumoFinanceiro resumo = calculadora.calcular(
                    List.of(new ItemPedido(CAMISETA, 1), new ItemPedido(CAMISETA, 1)),
                    TipoCliente.COMUM,
                    Cupom.DESC10);

            assertThat(resumo.subtotal()).isEqualByComparingTo("100.00");
            assertThat(resumo.desconto()).isEqualByComparingTo("10.00");
            assertThat(resumo.frete()).isEqualByComparingTo("20.00");
            assertThat(resumo.total()).isEqualByComparingTo("110.00");
        }
    }

    @Nested
    @DisplayName("desconto percentual")
    class Desconto {

        @Test
        @DisplayName("PLUS com DESC10 acumula 15% sobre o subtotal")
        void plusComDesc10Acumula15PorCento() {
            ResumoFinanceiro resumo = calculadora.calcular(
                    List.of(new ItemPedido(FONE, 1)),
                    TipoCliente.PLUS,
                    Cupom.DESC10);

            assertThat(resumo.desconto()).isEqualByComparingTo("45.00");
            assertThat(resumo.total()).isEqualByComparingTo("255.00");
        }

        @Nested
        @DisplayName("frete")
        class Frete {

        }

        @Test
        @DisplayName("Frete e gratis quando o subtotal atinge exatamente R$ 200,00")
        void freteGratisNoLimiteDoSubtotal() {
            ResumoFinanceiro resumo = calculadora.calcular(
                    List.of(new ItemPedido(CAMISETA, 4)),
                    TipoCliente.COMUM,
                    null);

            assertThat(resumo.subtotal()).isEqualByComparingTo("200.00");
            assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("Frete e cobrado quando o subtotal fica abaixo de R$ 200,00")
        void freteCobradoAbaixoDoLimite() {
            ResumoFinanceiro resumo = calculadora.calcular(
                    List.of(new ItemPedido(CAMISETA, 3)),
                    TipoCliente.COMUM,
                    null);

            assertThat(resumo.subtotal()).isEqualByComparingTo("150.00");
            assertThat(resumo.frete()).isEqualByComparingTo("20.00");
        }
    }
}
