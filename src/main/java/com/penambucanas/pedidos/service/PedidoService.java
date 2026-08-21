package com.penambucanas.pedidos.service;


import com.penambucanas.pedidos.api.dto.CriarPedidoRequest;
import com.penambucanas.pedidos.domain.Cliente;
import com.penambucanas.pedidos.domain.Cupom;
import com.penambucanas.pedidos.domain.ItemPedido;
import com.penambucanas.pedidos.domain.Pedido;
import com.penambucanas.pedidos.domain.Produto;
import com.penambucanas.pedidos.domain.StatusPedido;
import com.penambucanas.pedidos.domain.regras.CalculadoraPedido;
import com.penambucanas.pedidos.domain.regras.CalculadoraPontos;
import com.penambucanas.pedidos.domain.regras.ResumoFinanceiro;
import com.penambucanas.pedidos.exception.EstoqueInsuficienteException;
import com.penambucanas.pedidos.exception.RecursoNaoEncontradoException;
import com.penambucanas.pedidos.exception.RegraDeNegocioException;
import com.penambucanas.pedidos.messaging.PedidoPagoEvento;
import com.penambucanas.pedidos.messaging.PublicadorDeEventos;
import com.penambucanas.pedidos.repository.ClienteRepository;
import com.penambucanas.pedidos.repository.PedidoRepository;
import com.penambucanas.pedidos.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orquestra o ciclo de vida do pedido.
 *
 * O servico coordena (buscar, reservar estoque, salvar, publicar evento); quem
 * decide "quanto custa" e a {@link CalculadoraPedido}, "quantos pontos" e a
 * {@link CalculadoraPontos} e "pode mudar de status?" e o proprio
 * {@link Pedido}. Nenhuma dessas regras vive no controller.
 */
@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final CalculadoraPedido calculadoraPedido;
    private final CalculadoraPontos calculadoraPontos;
    private final PublicadorDeEventos publicadorDeEventos;

    public PedidoService(PedidoRepository pedidoRepository,
                         ProdutoRepository produtoRepository,
                         ClienteRepository clienteRepository,
                         CalculadoraPedido calculadoraPedido,
                         CalculadoraPontos calculadoraPontos,
                         PublicadorDeEventos publicadorDeEventos) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
        this.calculadoraPedido = calculadoraPedido;
        this.calculadoraPontos = calculadoraPontos;
        this.publicadorDeEventos = publicadorDeEventos;
    }

    /**
     * Cria o pedido: valida entrada e estoque, reserva o estoque e calcula os
     */
    @Transactional
    public Pedido criar(CriarPedidoRequest request) {
        Cliente cliente = buscarCliente(request.clienteId());
        Cupom cupom = Cupom.deCodigo(request.cupom());

        List<CriarPedidoRequest.ItemRequest> itensSolicitados = request.itens();
        validarItensSolicitados(itensSolicitados);

        Map<Long, Produto> produtosPorId = carregarProdutos(itensSolicitados);
        Map<Long, Integer> quantidadePorProduto = somarQuantidadesPorProduto(itensSolicitados);

        // RN-01: valida o pedido inteiro ANTES de reservar qualquer item.
        validarEstoque(produtosPorId, quantidadePorProduto);
        // RN-02: so entao o estoque e reservado.
        reservarEstoque(produtosPorId, quantidadePorProduto);

        Pedido pedido = new Pedido(cliente, cupom);
        itensSolicitados.forEach(item ->
                pedido.adicionarItem(new ItemPedido(produtosPorId.get(item.produtoId()), item.quantidade())));
        pedido.validarPossuiItens();

        ResumoFinanceiro resumo =
                calculadoraPedido.calcular(pedido.getItens(), cliente.getTipo(), cupom);
        pedido.aplicarResumoFinanceiro(resumo);

        return pedidoRepository.save(pedido);
    }

    /**
     * Paga o pedido, calcula os pontos e publica o evento
     * "pedido-pago" para que o consumidor credite os pontos no extrato.
     */
    @Transactional
    public Pedido pagar(Long pedidoId) {
        Pedido pedido = buscarPorId(pedidoId);

        int pontos = calculadoraPontos.calcular(pedido.getTotal(), pedido.getCliente().getTipo());
        pedido.pagar(pontos);
        Pedido pedidoPago = pedidoRepository.save(pedido);

        publicadorDeEventos.publicarPedidoPago(PedidoPagoEvento.de(pedidoPago));
        return pedidoPago;
    }

    /** cancelar devolve ao estoque as quantidades reservadas. */
    @Transactional
    public Pedido cancelar(Long pedidoId) {
        Pedido pedido = buscarPorId(pedidoId);
        pedido.cancelar();

        pedido.getItens().forEach(item -> {
            Produto produto = item.getProduto();
            produto.devolver(item.getQuantidade());
            produtoRepository.save(produto);
        });

        return pedidoRepository.save(pedido);
    }

    @Transactional(readOnly = true)
    public Pedido buscarPorId(Long pedidoId) {
        return pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", pedidoId));
    }

    /** Consulta com filtros opcionais por cliente e/ou status (diferencial 4.3). */
    @Transactional(readOnly = true)
    public List<Pedido> listar(Long clienteId, StatusPedido status) {
        if (clienteId != null && status != null) {
            return pedidoRepository.findByClienteIdAndStatus(clienteId, status);
        }
        if (clienteId != null) {
            return pedidoRepository.findByClienteId(clienteId);
        }
        if (status != null) {
            return pedidoRepository.findByStatus(status);
        }
        return pedidoRepository.findAll();
    }

    /** o cliente do pedido precisa existir. */
    private Cliente buscarCliente(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", clienteId));
    }

    /** reforcados fora da camada web. */
    private void validarItensSolicitados(List<CriarPedidoRequest.ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new RegraDeNegocioException("O pedido precisa ter pelo menos 1 item.");
        }
        boolean temQuantidadeInvalida = itens.stream().anyMatch(item -> item.quantidade() <= 0);
        if (temQuantidadeInvalida) {
            throw new RegraDeNegocioException("A quantidade de cada item deve ser maior que zero.");
        }
    }

    /**  todo produto referenciado precisa existir. */
    private Map<Long, Produto> carregarProdutos(List<CriarPedidoRequest.ItemRequest> itens) {
        Map<Long, Produto> produtosPorId = new LinkedHashMap<>();
        itens.forEach(item -> produtosPorId.computeIfAbsent(item.produtoId(),
                id -> produtoRepository.findById(id)
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", id))));
        return produtosPorId;
    }

    /**
     * O mesmo produto pode aparecer em mais de uma linha do pedido (caso C).
     * O estoque e validado pela quantidade somada, e nao linha a linha.
     */
    private Map<Long, Integer> somarQuantidadesPorProduto(List<CriarPedidoRequest.ItemRequest> itens) {
        Map<Long, Integer> quantidadePorProduto = new LinkedHashMap<>();
        itens.forEach(item -> quantidadePorProduto.merge(item.produtoId(), item.quantidade(), Integer::sum));
        return quantidadePorProduto;
    }

    /** confere se ha estoque para a quantidade somada de cada produto, sem reservar nada ainda. */
    private void validarEstoque(Map<Long, Produto> produtosPorId, Map<Long, Integer> quantidadePorProduto) {
        quantidadePorProduto.forEach((produtoId, quantidade) -> {
            Produto produto = produtosPorId.get(produtoId);
            if (!produto.temEstoquePara(quantidade)) {
                throw EstoqueInsuficienteException.para(
                        produto.getNome(), produtoId, quantidade, produto.getEstoque());
            }
        });
    }

    /** decrementa o estoque de cada produto e persiste a alteracao. */
    private void reservarEstoque(Map<Long, Produto> produtosPorId, Map<Long, Integer> quantidadePorProduto) {
        quantidadePorProduto.forEach((produtoId, quantidade) -> {
            Produto produto = produtosPorId.get(produtoId);
            produto.reservar(quantidade);
            produtoRepository.save(produto);
        });
    }
}
