package com.penambucanas.pedidos.domain.regras;

import com.penambucanas.pedidos.domain.TipoCliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
public class CalculadoraPontosTest {

    private final CalculadoraPontos calculadora = new CalculadoraPontos();

    /** valida a tabela de conversao total-pago -> pontos para varios totais e tipos de cliente. */
    @ParameterizedTest(name = "total {0} para cliente {1} gera {2} pontos")
    @CsvSource({
            // Casos de aceitacao A, B e C
            "200.00, COMUM, 200",
            "285.00, PLUS,  570",
            "110.00, COMUM, 110",
            // RN-10: apenas a parte inteira do total conta
            "150.90, COMUM, 150",
            "150.90, PLUS,  300",
            "0.99,   COMUM, 0",
            "0.00,   PLUS,  0"
    })
    void calculaPontosConformeTotalETipoDeCliente(BigDecimal total, TipoCliente tipo, int pontosEsperados) {
        assertThat(calculadora.calcular(total, tipo)).isEqualTo(pontosEsperados);
    }

    @Test
    @DisplayName("RN-11 - cliente PLUS pontua exatamente o dobro do COMUM")
    void plusPontuaEmDobro() {
        BigDecimal total = new BigDecimal("137.45");

        int pontosComum = calculadora.calcular(total, TipoCliente.COMUM);
        int pontosPlus = calculadora.calcular(total, TipoCliente.PLUS);

        assertThat(pontosComum).isEqualTo(137);
        assertThat(pontosPlus).isEqualTo(pontosComum * 2);
    }

    @Test
    @DisplayName("Total nulo nao gera pontos")
    void totalNuloNaoGeraPontos() {
        assertThat(calculadora.calcular(null, TipoCliente.PLUS)).isZero();
    }
}
