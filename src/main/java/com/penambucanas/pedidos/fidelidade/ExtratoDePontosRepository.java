package com.penambucanas.pedidos.fidelidade;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ExtratoDePontosRepository extends MongoRepository<LancamentoDePontos, String> {

    boolean existsByPedidoId(Long pedidoId);

    /** Todos os lancamentos de um cliente, do mais recente para o mais antigo. */
    List<LancamentoDePontos> findByClienteIdOrderByRegistradoEmDesc(Long clienteId);
}
