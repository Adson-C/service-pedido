package com.penambucanas.pedidos.repository;

import com.penambucanas.pedidos.domain.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
}
