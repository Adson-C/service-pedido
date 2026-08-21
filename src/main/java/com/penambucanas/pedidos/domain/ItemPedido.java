package com.penambucanas.pedidos.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "item_pedido")
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoUnitario;

    protected ItemPedido() {
    }

    public ItemPedido(Produto produto, int quantidade) {
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitario = produto.getPreco();
    }

    public BigDecimal getValorTotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    /** Associa este item ao pedido pai; chamado por {@link Pedido#adicionarItem}. */
    void vincularAo(Pedido pedido) {
        this.pedido = pedido;
    }

    /** @return o id do item, ou {@code null} se ainda nao persistido */
    public Long getId() {
        return id;
    }

    /** @return o pedido ao qual este item pertence */
    public Pedido getPedido() {
        return pedido;
    }

    /** @return o produto comprado nesta linha */
    public Produto getProduto() {
        return produto;
    }

    /** @return a quantidade comprada deste produto */
    public int getQuantidade() {
        return quantidade;
    }

    /** @return o preco unitario congelado no momento da criacao do pedido */
    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }
}
