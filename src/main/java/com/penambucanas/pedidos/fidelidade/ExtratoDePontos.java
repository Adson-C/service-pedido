package com.penambucanas.pedidos.fidelidade;


import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record ExtratoDePontos(Long clienteId,
                              int saldoPontos,
                              List<Lancamento> lancamentos) {

    public record Lancamento(Long pedidoId,
                             int pontos,
                             BigDecimal totalPago,
                             OffsetDateTime registradoEm) {
    }

    /** Monta o extrato somando os pontos de todos os lancamentos do cliente. */
    public static ExtratoDePontos de(Long clienteId, List<LancamentoDePontos> documentos) {
        int saldo = documentos.stream().mapToInt(LancamentoDePontos::getPontos).sum();
        List<Lancamento> lancamentos = documentos.stream()
                .map(doc -> new Lancamento(doc.getPedidoId(), doc.getPontos(),
                        doc.getTotalPago(), doc.getRegistradoEm()))
                .toList();
        return new ExtratoDePontos(clienteId, saldo, lancamentos);
    }
}
