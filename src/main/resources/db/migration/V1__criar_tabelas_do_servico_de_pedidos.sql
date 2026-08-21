-- V1: nucleo transacional do servico de pedidos.

create table cliente (
    id   bigserial    primary key,
    nome varchar(150) not null,
    tipo varchar(20)  not null,
    constraint ck_cliente_tipo check (tipo in ('COMUM', 'PLUS'))
);

create table produto (
    id        bigserial      primary key,
    nome      varchar(150)   not null,
    categoria varchar(60)    not null,
    preco     numeric(12, 2) not null,
    estoque   integer        not null,
    versao    bigint,
    constraint ck_produto_preco_nao_negativo   check (preco >= 0),
    constraint ck_produto_estoque_nao_negativo check (estoque >= 0)
);

create table pedido (
    id             bigserial      primary key,
    cliente_id     bigint         not null references cliente (id),
    cupom          varchar(20),
    status         varchar(20)    not null,
    subtotal       numeric(12, 2) not null,
    desconto       numeric(12, 2) not null,
    frete          numeric(12, 2) not null,
    total          numeric(12, 2) not null,
    pontos_gerados integer        not null default 0,
    criado_em      timestamptz    not null,
    pago_em        timestamptz,
    constraint ck_pedido_status check (status in ('CRIADO', 'PAGO', 'ENVIADO', 'ENTREGUE', 'CANCELADO')),
    constraint ck_pedido_cupom  check (cupom is null or cupom in ('DESC10', 'FRETEGRATIS')),
    -- RN-07: o total nunca pode ser negativo.
    constraint ck_pedido_total_nao_negativo check (total >= 0)
);

create table item_pedido (
    id             bigserial      primary key,
    pedido_id      bigint         not null references pedido (id),
    produto_id     bigint         not null references produto (id),
    quantidade     integer        not null,
    preco_unitario numeric(12, 2) not null,
    -- RN-13: a quantidade de cada item deve ser maior que zero.
    constraint ck_item_quantidade_positiva check (quantidade > 0)
);

create index ix_pedido_cliente_id   on pedido (cliente_id);
create index ix_pedido_status       on pedido (status);
create index ix_item_pedido_pedido  on item_pedido (pedido_id);
