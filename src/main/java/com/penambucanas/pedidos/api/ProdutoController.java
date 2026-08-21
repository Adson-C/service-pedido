package com.penambucanas.pedidos.api;

import com.penambucanas.pedidos.api.dto.ProdutoResponse;
import com.penambucanas.pedidos.service.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/produtos")
@Tag(name = "Produtos", description = "Catalogo da loja (endpoint de apoio)")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    /**
     * GET /produtos - lista todo o catalogo.
     *
     * @return todos os produtos cadastrados, com nome, preco e estoque atual
     */
    @GetMapping
    @Operation(summary = "Lista os produtos do catalogo com o estoque atual")
    public List<ProdutoResponse> listar() {
        return produtoService.listar().stream()
                .map(ProdutoResponse::de)
                .toList();
    }
}
