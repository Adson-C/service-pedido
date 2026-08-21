package com.penambucanas.pedidos.service;

import com.penambucanas.pedidos.api.dto.CriarPedidoRequest;
import com.penambucanas.pedidos.api.dto.CriarPedidoRequest.ItemRequest;
import com.penambucanas.pedidos.domain.Cliente;
import com.penambucanas.pedidos.domain.Cupom;
import com.penambucanas.pedidos.domain.ItemPedido;
import com.penambucanas.pedidos.domain.Pedido;
import com.penambucanas.pedidos.domain.Produto;
import com.penambucanas.pedidos.domain.StatusPedido;
import com.penambucanas.pedidos.domain.TipoCliente;
import com.penambucanas.pedidos.domain.regras.CalculadoraPedido;
import com.penambucanas.pedidos.domain.regras.CalculadoraPontos;
import com.penambucanas.pedidos.domain.regras.ResumoFinanceiro;
import com.penambucanas.pedidos.exception.CupomInvalidoException;
import com.penambucanas.pedidos.exception.EstoqueInsuficienteException;
import com.penambucanas.pedidos.exception.RecursoNaoEncontradoException;
import com.penambucanas.pedidos.exception.RegraDeNegocioException;
import com.penambucanas.pedidos.exception.TransicaoDeStatusInvalidaException;
import com.penambucanas.pedidos.messaging.PedidoPagoEvento;
import com.penambucanas.pedidos.messaging.PublicadorDeEventos;
import com.penambucanas.pedidos.repository.ClienteRepository;
import com.penambucanas.pedidos.repository.PedidoRepository;
import com.penambucanas.pedidos.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Os repositorios e o publicador de eventos sao mocks;
 * ponta a ponta dentro do servico, sem banco, sem broker e sem contexto Spring.
 */
@ExtendWith(MockitoExtension.class)
public class PedidoServiceTest {

    private static final Long ID_CLIENTE_COMUM = 1L;
    private static final Long ID_CLIENTE_PLUS = 2L;
    private static final Long ID_CAMISETA = 1L;
    private static final Long ID_TENIS = 2L;
    private static final Long ID_FONE = 3L;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private PublicadorDeEventos publicadorDeEventos;

    @Captor
    private ArgumentCaptor<PedidoPagoEvento> eventoCaptor;

    private PedidoService pedidoService;

    private Cliente clienteComum;
    private Cliente clientePlus;
    private Produto camiseta;
    private Produto tenis;
    private Produto fone;

    /** Monta o PedidoService com mocks para I/O e implementacoes reais das calculadoras, alem dos dados-base dos testes. */
    @BeforeEach
    void configurar() {
        pedidoService = new PedidoService(
                pedidoRepository,
                produtoRepository,
                clienteRepository,
                new CalculadoraPedido(),
                new CalculadoraPontos(),
                publicadorDeEventos);

        clienteComum = new Cliente(ID_CLIENTE_COMUM, "Giselle Pimentel", TipoCliente.COMUM);
        clientePlus = new Cliente(ID_CLIENTE_PLUS, "Adson Sá", TipoCliente.PLUS);
        camiseta = new Produto(ID_CAMISETA, "Camiseta", "VESTUARIO", new BigDecimal("50.00"), 10);
        tenis = new Produto(ID_TENIS, "Tenis", "CALCADOS", new BigDecimal("80.00"), 5);
        fone = new Produto(ID_FONE, "Fone", "ELETRONICOS", new BigDecimal("300.00"), 2);
    }

    /** Faz o mock de {@code pedidoRepository.save} devolver o mesmo objeto recebido, como um save real faria. */
    private void devolverPedidoSalvo() {
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(chamada -> chamada.getArgument(0));
    }

    @Nested
    @DisplayName("Criacao de pedido")
    class Criacao {

        /** Caso A ponta a ponta pelo servico: cria o pedido, calcula os valores e reserva o estoque de cada item. */
        @Test
        @DisplayName("Caso A - COMUM sem cupom: total 200,00 e estoque reservado")
        void casoA() {
            when(clienteRepository.findById(ID_CLIENTE_COMUM)).thenReturn(Optional.of(clienteComum));
            when(produtoRepository.findById(ID_CAMISETA)).thenReturn(Optional.of(camiseta));
            when(produtoRepository.findById(ID_TENIS)).thenReturn(Optional.of(tenis));
            devolverPedidoSalvo();

            Pedido pedido = pedidoService.criar(new CriarPedidoRequest(
                    ID_CLIENTE_COMUM,
                    List.of(new ItemRequest(ID_CAMISETA, 2), new ItemRequest(ID_TENIS, 1)),
                    null));

            assertThat(pedido.getSubtotal()).isEqualByComparingTo("180.00");
            assertThat(pedido.getDesconto()).isEqualByComparingTo("0.00");
            assertThat(pedido.getFrete()).isEqualByComparingTo("20.00");
            assertThat(pedido.getTotal()).isEqualByComparingTo("200.00");
            assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CRIADO);

            // RN-02: o estoque foi reservado
            assertThat(camiseta.getEstoque()).isEqualTo(8);
            assertThat(tenis.getEstoque()).isEqualTo(4);
            verify(produtoRepository, times(2)).save(any(Produto.class));
        }

        /** Caso B ponta a ponta: cliente PLUS com cupom FRETEGRATIS, cupom persistido e estoque do Fone reservado. */
        @Test
        @DisplayName("Caso B - PLUS com FRETEGRATIS: total 285,00")
        void casoB() {
            when(clienteRepository.findById(ID_CLIENTE_PLUS)).thenReturn(Optional.of(clientePlus));
            when(produtoRepository.findById(ID_FONE)).thenReturn(Optional.of(fone));
            devolverPedidoSalvo();

            Pedido pedido = pedidoService.criar(new CriarPedidoRequest(
                    ID_CLIENTE_PLUS,
                    List.of(new ItemRequest(ID_FONE, 1)),
                    "FRETEGRATIS"));

            assertThat(pedido.getSubtotal()).isEqualByComparingTo("300.00");
            assertThat(pedido.getDesconto()).isEqualByComparingTo("15.00");
            assertThat(pedido.getFrete()).isEqualByComparingTo("0.00");
            assertThat(pedido.getTotal()).isEqualByComparingTo("285.00");
            assertThat(pedido.getCupom()).isEqualTo(Cupom.FRETEGRATIS);
            assertThat(fone.getEstoque()).isEqualTo(1);
        }

        /** Caso C ponta a ponta: o mesmo produto em duas linhas do pedido deve ter as quantidades somadas ao reservar estoque. */
        @Test
        @DisplayName("Caso C - COMUM com DESC10 e o mesmo produto em duas linhas: total 110,00")
        void casoC() {
            when(clienteRepository.findById(ID_CLIENTE_COMUM)).thenReturn(Optional.of(clienteComum));
            when(produtoRepository.findById(ID_CAMISETA)).thenReturn(Optional.of(camiseta));
            devolverPedidoSalvo();

            Pedido pedido = pedidoService.criar(new CriarPedidoRequest(
                    ID_CLIENTE_COMUM,
                    List.of(new ItemRequest(ID_CAMISETA, 1), new ItemRequest(ID_CAMISETA, 1)),
                    "DESC10"));

            assertThat(pedido.getSubtotal()).isEqualByComparingTo("100.00");
            assertThat(pedido.getDesconto()).isEqualByComparingTo("10.00");
            assertThat(pedido.getFrete()).isEqualByComparingTo("20.00");
            assertThat(pedido.getTotal()).isEqualByComparingTo("110.00");
            // as duas linhas somam 2 unidades reservadas
            assertThat(camiseta.getEstoque()).isEqualTo(8);
        }

        /** Caso D ponta a ponta: estoque insuficiente lanca excecao com os numeros certos, e nada e salvo (nem produto, nem pedido). */
        @Test
        @DisplayName("Caso D - estoque insuficiente rejeita o pedido e nao reserva nada")
        void casoD() {
            when(clienteRepository.findById(ID_CLIENTE_COMUM)).thenReturn(Optional.of(clienteComum));
            when(produtoRepository.findById(ID_FONE)).thenReturn(Optional.of(fone));

            CriarPedidoRequest request = new CriarPedidoRequest(
                    ID_CLIENTE_COMUM, List.of(new ItemRequest(ID_FONE, 3)), null);

            assertThatThrownBy(() -> pedidoService.criar(request))
                    .isInstanceOf(EstoqueInsuficienteException.class)
                    .hasMessageContaining("Fone")
                    .hasMessageContaining("solicitado 3")
                    .hasMessageContaining("disponivel 2");

            assertThat(fone.getEstoque()).isEqualTo(2);
            verify(produtoRepository, never()).save(any(Produto.class));
            verify(pedidoRepository, never()).save(any(Pedido.class));
        }

        @Test
        @DisplayName("Estoque insuficiente em um item derruba o pedido inteiro (nada e reservado)")
        void estoqueInsuficienteEmUmItemDerrubaPedidoInteiro() {
            when(clienteRepository.findById(ID_CLIENTE_COMUM)).thenReturn(Optional.of(clienteComum));
            when(produtoRepository.findById(ID_CAMISETA)).thenReturn(Optional.of(camiseta));
            when(produtoRepository.findById(ID_FONE)).thenReturn(Optional.of(fone));

            CriarPedidoRequest request = new CriarPedidoRequest(
                    ID_CLIENTE_COMUM,
                    List.of(new ItemRequest(ID_CAMISETA, 1), new ItemRequest(ID_FONE, 5)),
                    null);

            assertThatThrownBy(() -> pedidoService.criar(request))
                    .isInstanceOf(EstoqueInsuficienteException.class);

            assertThat(camiseta.getEstoque()).isEqualTo(10);
            assertThat(fone.getEstoque()).isEqualTo(2);
            verify(produtoRepository, never()).save(any(Produto.class));
        }

        @Test
        @DisplayName("cliente inexistente resulta em recurso nao encontrado")
        void clienteInexistente() {
            when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

            CriarPedidoRequest request = new CriarPedidoRequest(
                    99L, List.of(new ItemRequest(ID_CAMISETA, 1)), null);

            assertThatThrownBy(() -> pedidoService.criar(request))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessageContaining("Cliente");
        }

        @Test
        @DisplayName("produto inexistente resulta em recurso nao encontrado")
        void produtoInexistente() {
            when(clienteRepository.findById(ID_CLIENTE_COMUM)).thenReturn(Optional.of(clienteComum));
            when(produtoRepository.findById(99L)).thenReturn(Optional.empty());

            CriarPedidoRequest request = new CriarPedidoRequest(
                    ID_CLIENTE_COMUM, List.of(new ItemRequest(99L, 1)), null);

            assertThatThrownBy(() -> pedidoService.criar(request))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessageContaining("Produto");
        }

        @Test
        @DisplayName("pedido sem itens e rejeitado")
        void pedidoSemItens() {
            when(clienteRepository.findById(ID_CLIENTE_COMUM)).thenReturn(Optional.of(clienteComum));

            CriarPedidoRequest request = new CriarPedidoRequest(ID_CLIENTE_COMUM, List.of(), null);

            assertThatThrownBy(() -> pedidoService.criar(request))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("pelo menos 1 item");
        }

        @Test
        @DisplayName("RN-13 - quantidade zero ou negativa e rejeitada")
        void quantidadeInvalida() {
            when(clienteRepository.findById(ID_CLIENTE_COMUM)).thenReturn(Optional.of(clienteComum));

            CriarPedidoRequest request = new CriarPedidoRequest(
                    ID_CLIENTE_COMUM, List.of(new ItemRequest(ID_CAMISETA, 0)), null);

            assertThatThrownBy(() -> pedidoService.criar(request))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("maior que zero");
        }
    }

    @Nested
    @DisplayName("Pagamento")
    class Pagamento {

        @Test
        @DisplayName("Caso A - pagar credita 200 pontos e publica o evento pedido-pago")
        void pagarPedidoDeClienteComum() {
            Pedido pedido = pedidoCriado(10L, clienteComum, "200.00");
            when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));
            devolverPedidoSalvo();

            Pedido pago = pedidoService.pagar(10L);

            assertThat(pago.getStatus()).isEqualTo(StatusPedido.PAGO);
            assertThat(pago.getPontosGerados()).isEqualTo(200);

            verify(publicadorDeEventos).publicarPedidoPago(eventoCaptor.capture());
            PedidoPagoEvento evento = eventoCaptor.getValue();
            assertThat(evento.pedidoId()).isEqualTo(10L);
            assertThat(evento.clienteId()).isEqualTo(ID_CLIENTE_COMUM);
            assertThat(evento.pontosGerados()).isEqualTo(200);
            assertThat(evento.totalPago()).isEqualByComparingTo("200.00");
            assertThat(evento.eventoId()).isNotBlank();
        }

        @Test
        @DisplayName("Caso B - cliente PLUS pontua em dobro: 285,00 gera 570 pontos")
        void pagarPedidoDeClientePlus() {
            Pedido pedido = pedidoCriado(11L, clientePlus, "285.00");
            when(pedidoRepository.findById(11L)).thenReturn(Optional.of(pedido));
            devolverPedidoSalvo();

            Pedido pago = pedidoService.pagar(11L);

            assertThat(pago.getPontosGerados()).isEqualTo(570);
        }

        @Test
        @DisplayName("Pedido inexistente resulta em 404")
        void pedidoInexistente() {
            when(pedidoRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pedidoService.pagar(404L))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
        }
    }

    @Nested
    @DisplayName("Cancelamento")
    class Cancelamento {

        @Test
        @DisplayName("cancelar devolve ao estoque as quantidades reservadas")
        void cancelarDevolveEstoque() {
            camiseta.reservar(2);
            tenis.reservar(1);
            Pedido pedido = new Pedido(20L, clienteComum, null);
            pedido.adicionarItem(new ItemPedido(camiseta, 2));
            pedido.adicionarItem(new ItemPedido(tenis, 1));
            when(pedidoRepository.findById(20L)).thenReturn(Optional.of(pedido));
            devolverPedidoSalvo();

            Pedido cancelado = pedidoService.cancelar(20L);

            assertThat(cancelado.getStatus()).isEqualTo(StatusPedido.CANCELADO);
            assertThat(camiseta.getEstoque()).isEqualTo(10);
            assertThat(tenis.getEstoque()).isEqualTo(5);
            verify(produtoRepository, times(2)).save(any(Produto.class));
        }

        @Test
        @DisplayName("Pedido PAGO pode ser cancelado e devolve o estoque")
        void cancelarPedidoPago() {
            camiseta.reservar(1);
            Pedido pedido = new Pedido(21L, clienteComum, null);
            pedido.adicionarItem(new ItemPedido(camiseta, 1));
            pedido.pagar(50);
            when(pedidoRepository.findById(21L)).thenReturn(Optional.of(pedido));
            devolverPedidoSalvo();

            pedidoService.cancelar(21L);

            assertThat(camiseta.getEstoque()).isEqualTo(10);
        }

        @Test
        @DisplayName("Pedido ENVIADO nao pode ser cancelado e o estoque nao muda")
        void naoCancelaPedidoEnviado() {
            camiseta.reservar(1);
            Pedido pedido = new Pedido(22L, clienteComum, null);
            pedido.adicionarItem(new ItemPedido(camiseta, 1));
            pedido.pagar(50);
            pedido.enviar();
            when(pedidoRepository.findById(22L)).thenReturn(Optional.of(pedido));

            assertThatThrownBy(() -> pedidoService.cancelar(22L))
                    .isInstanceOf(TransicaoDeStatusInvalidaException.class);

            assertThat(camiseta.getEstoque()).isEqualTo(9);
            verify(produtoRepository, never()).save(any(Produto.class));
        }
    }

    @Nested
    @DisplayName("Consulta com filtros")
    class Consulta {

        @Test
        @DisplayName("Filtra por cliente e status quando os dois sao informados")
        void filtraPorClienteEStatus() {
            when(pedidoRepository.findByClienteIdAndStatus(ID_CLIENTE_COMUM, StatusPedido.PAGO))
                    .thenReturn(List.of(pedidoCriado(30L, clienteComum, "200.00")));

            assertThat(pedidoService.listar(ID_CLIENTE_COMUM, StatusPedido.PAGO)).hasSize(1);
        }

        @Test
        @DisplayName("Sem filtros, lista todos os pedidos")
        void semFiltrosListaTudo() {
            when(pedidoRepository.findAll()).thenReturn(List.of());

            assertThat(pedidoService.listar(null, null)).isEmpty();
            verify(pedidoRepository).findAll();
        }
    }

    /** Pedido ja criado e valorizado, como sairia do banco. */
    private Pedido pedidoCriado(Long id, Cliente cliente, String total) {
        Pedido pedido = new Pedido(id, cliente, null);
        pedido.adicionarItem(new ItemPedido(camiseta, 1));
        pedido.aplicarResumoFinanceiro(new ResumoFinanceiro(
                new BigDecimal(total), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal(total)));
        return pedido;
    }
}
