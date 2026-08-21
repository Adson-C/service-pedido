package com.penambucanas.pedidos.domain;

import com.penambucanas.pedidos.exception.CupomInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CupomTest {

    @ParameterizedTest
    @ValueSource(strings = {"DESC10", "desc10", " DESC10 "})
    @DisplayName("Reconhece DESC10 independente de caixa e espacos")
    void reconheceDesc10(String codigo) {
        assertThat(Cupom.deCodigo(codigo)).isEqualTo(Cupom.DESC10);
    }

    @Test
    @DisplayName("FRETEGRATIS isenta o frete e nao da desconto percentual")
    void fretegratisIsentaFrete() {
        Cupom cupom = Cupom.deCodigo("FRETEGRATIS");

        assertThat(cupom.isentaFrete()).isTrue();
        assertThat(cupom.getPercentualDesconto()).isEqualByComparingTo("0");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Ausencia de cupom e valida e resulta em nenhum cupom")
    void semCupom(String codigo) {
        assertThat(Cupom.deCodigo(codigo)).isNull();
    }

    @Test
    @DisplayName("Cupom inexistente e rejeitado com mensagem clara")
    void cupomInexistenteERejeitado() {
        assertThatThrownBy(() -> Cupom.deCodigo("BLACKFRIDAY"))
                .isInstanceOf(CupomInvalidoException.class)
                .hasMessageContaining("BLACKFRIDAY")
                .hasMessageContaining("DESC10")
                .hasMessageContaining("FRETEGRATIS");
    }
}
