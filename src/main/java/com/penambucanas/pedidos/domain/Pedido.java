package com.penambucanas.pedidos.domain;

import com.penambucanas.pedidos.domain.regras.ResumoFinanceiro;
import com.penambucanas.pedidos.exception.RegraDeNegocioException;
import com.penambucanas.pedidos.exception.TransicaoDeStatusInvalidaException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "pedido")
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ItemPedido> itens = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Cupom cupom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPedido status = StatusPedido.CRIADO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal desconto = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal frete = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "pontos_gerados", nullable = false)
    private int pontosGerados;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "pago_em")
    private OffsetDateTime pagoEm;

    protected Pedido() {
    }

    public Pedido(Cliente cliente, Cupom cupom) {
        this.cliente = cliente;
        this.cupom = cupom;
    }

    public Pedido(Long id, Cliente cliente, Cupom cupom) {
        this(cliente, cupom);
        this.id = id;
    }

    public void adicionarItem(ItemPedido item) {
        item.vincularAo(this);
        this.itens.add(item);
    }

    /** Grava no pedido os valores calculados pela regra de precificacao. */
    public void aplicarResumoFinanceiro(ResumoFinanceiro resumo) {
        this.subtotal = resumo.subtotal();
        this.desconto = resumo.desconto();
        this.frete = resumo.frete();
        this.total = resumo.total();
    }

    public void pagar(int pontosGerados) {
        transitarPara(StatusPedido.PAGO);
        this.pontosGerados = pontosGerados;
        this.pagoEm = OffsetDateTime.now();
    }

    public void cancelar() {
        transitarPara(StatusPedido.CANCELADO);
    }

    public void enviar() {
        transitarPara(StatusPedido.ENVIADO);
    }

    public void entregar() {
        transitarPara(StatusPedido.ENTREGUE);
    }

    private void transitarPara(StatusPedido destino) {
        if (!status.podeTransitarPara(destino)) {
            throw new TransicaoDeStatusInvalidaException(status, destino);
        }
        this.status = destino;
    }

    /** um pedido precisa ter pelo menos um item. */
    public void validarPossuiItens() {
        if (itens.isEmpty()) {
            throw new RegraDeNegocioException("O pedido precisa ter pelo menos 1 item.");
        }
    }

    /** @return o id do pedido, ou {@code null} se ainda nao persistido */
    public Long getId() {
        return id;
    }

    /** @return o cliente dono do pedido */
    public Cliente getCliente() {
        return cliente;
    }

    /** @return lista imutavel dos itens do pedido, para impedir alteracao por fora de {@link #adicionarItem} */
    public List<ItemPedido> getItens() {
        return Collections.unmodifiableList(itens);
    }

    /** @return o cupom aplicado, ou {@code null} se nenhum foi usado */
    public Cupom getCupom() {
        return cupom;
    }

    /** @return o status atual na maquina de estados (secao 2.3) */
    public StatusPedido getStatus() {
        return status;
    }

    /** @return o subtotal calculado (soma dos itens, RN-04) */
    public BigDecimal getSubtotal() {
        return subtotal;
    }

    /** @return o desconto calculado (RN-05) */
    public BigDecimal getDesconto() {
        return desconto;
    }

    /** @return o valor de frete calculado (RN-06) */
    public BigDecimal getFrete() {
        return frete;
    }

    /** @return o total final do pedido (RN-07) */
    public BigDecimal getTotal() {
        return total;
    }

    /** @return os pontos de fidelidade gerados ao pagar (RN-09 a RN-11); 0 enquanto nao pago */
    public int getPontosGerados() {
        return pontosGerados;
    }

    /** @return a data/hora de criacao do pedido */
    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    /** @return a data/hora do pagamento, ou {@code null} se ainda nao pago */
    public OffsetDateTime getPagoEm() {
        return pagoEm;
    }
}
