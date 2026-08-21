package com.penambucanas.pedidos.api;

import com.penambucanas.pedidos.fidelidade.ExtratoDePontos;
import com.penambucanas.pedidos.fidelidade.FidelidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST do programa de fidelidade do cliente.
 * Apenas traduz HTTP para chamadas de servico; a regra fica em {@link FidelidadeService}.
 */
@RestController
@RequestMapping("/clientes")
@Tag(name = "Clientes", description = "Programa de fidelidade")
public class ClienteController {

    private final FidelidadeService fidelidadeService;

    public ClienteController(FidelidadeService fidelidadeService) {
        this.fidelidadeService = fidelidadeService;
    }

    /**
     * GET /clientes/{id}/pontos - devolve o saldo e o historico de pontos do cliente.
     *
     * @param id id do cliente
     * @return extrato com saldo total e lista de lancamentos, lido do MongoDB
     */
    @GetMapping("/{id}/pontos")
    @Operation(summary = "Extrato de pontos do cliente (lido do MongoDB)")
    public ExtratoDePontos extratoDePontos(@PathVariable Long id) {
        return fidelidadeService.consultarExtrato(id);
    }
}
