package com.penambucanas.pedidos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCliente tipo;

    public Cliente() {
        // exigido pelo JPA
    }

    public Cliente(String nome, TipoCliente tipo) {
        this.nome = nome;
        this.tipo = tipo;
    }

    public Cliente(Long id, String nome, TipoCliente tipo) {
        this(nome, tipo);
        this.id = id;
    }

    /** {@code true} se o cliente e do tipo PLUS. */
    public boolean isPlus() {
        return tipo != null && tipo.isPlus();
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public TipoCliente getTipo() {
        return tipo;
    }
}
