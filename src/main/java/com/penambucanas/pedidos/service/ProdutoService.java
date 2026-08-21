package com.penambucanas.pedidos.service;

import com.penambucanas.pedidos.domain.Produto;
import com.penambucanas.pedidos.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProdutoService {
    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    /** @return todos os produtos cadastrados no catalogo */
    @Transactional(readOnly = true)
    public List<Produto> listar() {
        return produtoRepository.findAll();
    }
}
