package com.penambucanas.pedidos.domain.regras;


import com.penambucanas.pedidos.domain.Cliente;

import com.penambucanas.pedidos.domain.TipoCliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

public class ClienteTest {

    @Test
    @DisplayName("Construtor com id preenche id, nome e tipo")
    void criaClienteComId() {
        Cliente cliente = new Cliente(7L, "Adson", TipoCliente.PLUS);

        assertThat(cliente.getId()).isEqualTo(7L);
        assertThat(cliente.getNome()).isEqualTo("Adson");
        assertThat(cliente.getTipo()).isEqualTo(TipoCliente.PLUS);
    }

    @Test
    @DisplayName("isPlus() e true para PLUS e false para COMUM")
    void isPlusRefleteOTipo() {
        assertThat(new Cliente(1L, "Giselle", TipoCliente.PLUS).isPlus()).isTrue();
        assertThat(new Cliente(2L, "Adson", TipoCliente.COMUM).isPlus()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(TipoCliente.class)
    @DisplayName("isPlus() do cliente acompanha o tipo em todos os valores do enum")
    void isPlusAcompanhaOEnum(TipoCliente tipo) {
        assertThat(new Cliente("Carla", tipo).isPlus()).isEqualTo(tipo.isPlus());
    }

    @Test
    @DisplayName("Cliente sem tipo (construtor do JPA) nao e PLUS")
    void clienteSemTipoNaoEPlus() {
        Cliente cliente = new Cliente(); // construtor protegido, visivel por estarmos no mesmo pacote

        assertThat(cliente.getTipo()).isNull();
        assertThat(cliente.isPlus()).isFalse();
    }
}
