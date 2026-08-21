package com.penambucanas.pedidos.repository;

import com.penambucanas.pedidos.domain.Pedido;
import com.penambucanas.pedidos.domain.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio Spring Data JPA para {@link Pedido}.
 * As consultas de filtro (diferencial 4.3) sao geradas automaticamente pelo nome do metodo.
 */
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByClienteId(Long clienteId);

    List<Pedido> findByStatus(StatusPedido status);

    /** Pedidos de um cliente especifico em um determinado status. */
    List<Pedido> findByClienteIdAndStatus(Long clienteId, StatusPedido status);
}
