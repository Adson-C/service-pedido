package com.penambucanas.pedidos.domain;

import com.penambucanas.pedidos.exception.EstoqueInsuficienteException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;

@Entity
@Table(name = "produto")
public class Produto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, length = 60)
    private String categoria;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Column(nullable = false)
    private int estoque;

    @Version
    private Long versao;

    protected Produto() {
    }

    public Produto(String nome, String categoria, BigDecimal preco, int estoque) {
        this.nome = nome;
        this.categoria = categoria;
        this.preco = preco;
        this.estoque = estoque;
    }

    public Produto(Long id, String nome, String categoria, BigDecimal preco, int estoque) {
        this(nome, categoria, preco, estoque);
        this.id = id;
    }

    /** ha estoque suficiente para a quantidade pedida? */
    public boolean temEstoquePara(int quantidade) {
        return estoque >= quantidade;
    }

    /** reserva (decrementa) o estoque ao criar o pedido. */
    public void reservar(int quantidade) {
        if (!temEstoquePara(quantidade)) {
            throw EstoqueInsuficienteException.para(nome, id, quantidade, estoque);
        }
        this.estoque -= quantidade;
    }

    /** devolve ao estoque a quantidade reservada, ao cancelar o pedido. */
    public void devolver(int quantidade) {
        this.estoque += quantidade;
    }

    /** @return o id do produto, ou {@code null} se ainda nao persistido */
    public Long getId() {
        return id;
    }

    /** @return o nome exibido do produto */
    public String getNome() {
        return nome;
    }

    /** @return a categoria do produto (ex.: "VESTUARIO", "ELETRONICOS") */
    public String getCategoria() {
        return categoria;
    }

    /** @return o preco unitario atual do produto */
    public BigDecimal getPreco() {
        return preco;
    }

    /** @return a quantidade disponivel em estoque no momento */
    public int getEstoque() {
        return estoque;
    }
}
