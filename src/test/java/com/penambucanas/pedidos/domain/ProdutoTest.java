package com.penambucanas.pedidos.domain;

import com.penambucanas.pedidos.exception.EstoqueInsuficienteException;
import com.penambucanas.pedidos.exception.RegraDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ProdutoTest {

    private static Produto comEstoque(int estoque) {
        return new Produto(42L, "Camisa Polo", "VESTUARIO", new BigDecimal("99.90"), estoque);
    }

    @ParameterizedTest
    @CsvSource({"10, 1, true", "10, 9, true", "10, 10, true", "10, 11, false", "0, 1, false", "0, 0, true"})
    @DisplayName("temEstoquePara() so e falso quando a quantidade passa do disponivel")
    void temEstoqueParaRespeitaOLimite(int estoque, int quantidade, boolean esperado) {
        assertThat(comEstoque(estoque).temEstoquePara(quantidade)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("Reservar decrementa o estoque na quantidade pedida")
    void reservarDecrementaEstoque() {
        Produto produto = comEstoque(10);

        produto.reservar(3);

        assertThat(produto.getEstoque()).isEqualTo(7);
    }

    /** reservar todo o disponivel e valido e deixa o estoque zerado, sem lancar excecao. */
    @Test
    @DisplayName("Reservar exatamente todo o estoque zera o disponivel")
    void reservarTodoOEstoqueZera() {
        Produto produto = comEstoque(5);

        produto.reservar(5);

        assertThat(produto.getEstoque()).isZero();
        assertThat(produto.temEstoquePara(1)).isFalse();
    }

    /** reservas sucessivas se acumulam, cada uma partindo do estoque ja decrementado. */
    @Test
    @DisplayName("Reservas sucessivas descontam do estoque restante")
    void reservasSucessivasAcumulam() {
        Produto produto = comEstoque(10);

        produto.reservar(4);
        produto.reservar(3);

        assertThat(produto.getEstoque()).isEqualTo(3);
    }

    @Test
    @DisplayName("Reservar acima do estoque lanca EstoqueInsuficienteException")
    void reservarAcimaDoEstoqueLancaExcecao() {
        Produto produto = comEstoque(2);

        assertThatThrownBy(() -> produto.reservar(3))
                .isInstanceOf(EstoqueInsuficienteException.class)
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    @DisplayName("Mensagem do erro cita produto, id, solicitado e disponivel")
    void mensagemDoErroDetalhaOEstoque() {
        Produto produto = comEstoque(2);

        assertThatThrownBy(() -> produto.reservar(3))
                .hasMessageContaining("Camisa Polo")
                .hasMessageContaining("42")
                .hasMessageContaining("solicitado 3")
                .hasMessageContaining("disponivel 2");
    }

    @Test
    @DisplayName("Reserva recusada nao altera o estoque")
    void reservaRecusadaNaoAlteraEstoque() {
        Produto produto = comEstoque(2);

        assertThatThrownBy(() -> produto.reservar(3)).isInstanceOf(EstoqueInsuficienteException.class);

        assertThat(produto.getEstoque()).isEqualTo(2);
    }

    @Test
    @DisplayName("Produto sem estoque recusa ate uma unidade")
    void produtoZeradoRecusaQualquerReserva() {
        Produto produto = comEstoque(0);

        assertThatThrownBy(() -> produto.reservar(1))
                .isInstanceOf(EstoqueInsuficienteException.class)
                .hasMessageContaining("disponivel 0");

        assertThat(produto.getEstoque()).isZero();
    }

    @Test
    @DisplayName("Devolver incrementa o estoque na quantidade devolvida")
    void devolverIncrementaEstoque() {
        Produto produto = comEstoque(4);

        produto.devolver(6);

        assertThat(produto.getEstoque()).isEqualTo(10);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10})
    @DisplayName("Reservar e devolver a mesma quantidade restaura o estoque original")
    void reservaSeguidaDeDevolucaoRestauraEstoque(int quantidade) {
        Produto produto = comEstoque(10);

        produto.reservar(quantidade);
        produto.devolver(quantidade);

        assertThat(produto.getEstoque()).isEqualTo(10);
    }

    @Test
    @DisplayName("Estoque devolvido volta a ser reservavel")
    void estoqueDevolvidoVoltaASerReservavel() {
        Produto produto = comEstoque(3);
        produto.reservar(3);

        produto.devolver(3);

        assertThat(produto.temEstoquePara(3)).isTrue();
        produto.reservar(3);
        assertThat(produto.getEstoque()).isZero();
    }

    @Test
    @DisplayName("Reserva e devolucao nao alteram os demais dados do produto")
    void movimentacaoNaoAlteraDadosDoCatalogo() {
        Produto produto = comEstoque(10);

        produto.reservar(4);
        produto.devolver(2);

        assertThat(produto.getId()).isEqualTo(42L);
        assertThat(produto.getNome()).isEqualTo("Camisa Polo");
        assertThat(produto.getCategoria()).isEqualTo("VESTUARIO");
        assertThat(produto.getPreco()).isEqualByComparingTo("99.90");
        assertThat(produto.getEstoque()).isEqualTo(8);
    }
}
