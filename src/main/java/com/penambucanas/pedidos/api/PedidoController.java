package com.penambucanas.pedidos.api;


import com.penambucanas.pedidos.api.dto.CriarPedidoRequest;
import com.penambucanas.pedidos.api.dto.PedidoResponse;
import com.penambucanas.pedidos.domain.Pedido;
import com.penambucanas.pedidos.domain.StatusPedido;
import com.penambucanas.pedidos.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/pedidos")
@Tag(name = "Pedidos", description = "Criacao, consulta, pagamento e cancelamento de pedidos")
public class PedidoController {


    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    /**
     * POST /pedidos - cria um pedido novo.
     *
     * @param request   cliente, itens e cupom opcional; validado via Bean Validation
     * @param uriBuilder usado para montar a URL do recurso criado (cabecalho Location)
     * @return 201 Created com o pedido criado e o header {@code Location: /pedidos/{id}}
     */
    @PostMapping
    @Operation(summary = "Cria um pedido: valida entrada e estoque, reserva o estoque e calcula os valores")
    public ResponseEntity<PedidoResponse> criar(@Valid @RequestBody CriarPedidoRequest request,
                                                UriComponentsBuilder uriBuilder) {
        Pedido pedido = pedidoService.criar(request);
        URI location = uriBuilder.path("/pedidos/{id}").buildAndExpand(pedido.getId()).toUri();
        return ResponseEntity.created(location).body(PedidoResponse.de(pedido));
    }

    /**
     * GET /pedidos/{id} - busca um pedido pelo id.
     *
     * @param id id do pedido
     * @return o pedido encontrado; lanca {@code RecursoNaoEncontradoException} (404) se nao existir
     */
    @GetMapping("/{id}")
    @Operation(summary = "Consulta um pedido pelo id")
    public PedidoResponse buscar(@PathVariable Long id) {
        return PedidoResponse.de(pedidoService.buscarPorId(id));
    }

    /**
     * GET /pedidos - lista pedidos, com filtros opcionais.
     *
     * @param clienteId filtra por cliente, se informado
     * @param status    filtra por status, se informado
     * @return lista de pedidos que atendem aos filtros informados (ou todos, se nenhum filtro for passado)
     */
    @GetMapping
    @Operation(summary = "Lista pedidos, com filtro opcional por cliente e/ou status")
    public List<PedidoResponse> listar(@RequestParam(required = false) Long clienteId,
                                       @RequestParam(required = false) StatusPedido status) {
        return pedidoService.listar(clienteId, status).stream()
                .map(PedidoResponse::de)
                .toList();
    }

    /**
     * POST /pedidos/{id}/pagamento - paga um pedido em status CRIADO.
     *
     * @param id id do pedido a pagar
     * @return o pedido atualizado, agora em status PAGO, com os pontos gerados
     */
    @PostMapping("/{id}/pagamento")
    @Operation(summary = "Paga o pedido: gera pontos e publica o evento pedido-pago")
    public PedidoResponse pagar(@PathVariable Long id) {
        return PedidoResponse.de(pedidoService.pagar(id));
    }

    /**
     * POST /pedidos/{id}/cancelamento - cancela um pedido em CRIADO ou PAGO.
     *
     * @param id id do pedido a cancelar
     * @return o pedido atualizado, agora em status CANCELADO
     */
    @PostMapping("/{id}/cancelamento")
    @Operation(summary = "Cancela o pedido e devolve as quantidades ao estoque")
    public PedidoResponse cancelar(@PathVariable Long id) {
        return PedidoResponse.de(pedidoService.cancelar(id));
    }
}
