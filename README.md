# Pedidos

Serviço de pedidos para um cenário das Lojas Pernambucanas: cria pedidos, reserva
estoque, calcula preços (desconto, frete e total), controla o ciclo de vida do
pedido (status) e credita pontos de fidelidade de forma assíncrona quando o
pedido é pago.

Desenvolvido em **Java 17 + Spring Boot**
---

## Sumário

- [Arquitetura](#arquitetura)
- [Stack técnica](#stack-técnica)
- [Modelo de domínio](#modelo-de-domínio)
- [Regras de negócio](#regras-de-negócio)
- [Máquina de estados do pedido](#máquina-de-estados-do-pedido)
- [Fluxo assíncrono de fidelidade](#fluxo-assíncrono-de-fidelidade)
- [Endpoints da API](#endpoints-da-api)
- [Tratamento de erros](#tratamento-de-erros)
- [Como rodar](#como-rodar)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Testes](#testes)
- [Estrutura de pastas](#estrutura-de-pastas)

---

## Arquitetura

O projeto segue uma separação clássica em camadas, com a regra de negócio
isolada do framework (sem dependência de Spring nas classes de `domain.regras`):

```
api/            Controllers REST, DTOs de request/response e o exception handler global
service/        Orquestração de casos de uso (busca, valida, chama o domínio, persiste)
domain/         Entidades JPA (Cliente, Produto, Pedido, ItemPedido) e enums (StatusPedido, Cupom, TipoCliente)
domain/regras/  Regras de cálculo puras e testáveis (CalculadoraPedido, CalculadoraPontos, RegrasDoPedido)
messaging/      Contrato de publicação de eventos (PublicadorDeEventos) + implementação RabbitMQ
fidelidade/     Módulo de pontos: entidade Mongo, repositório e serviço de crédito idempotente
repository/     Repositórios Spring Data JPA
exception/      Exceções de domínio mapeadas para respostas HTTP
```

Ideia central: o `PedidoController` só traduz HTTP; o `PedidoService` orquestra
(busca cliente/produto, reserva estoque, salva, publica evento); e quem decide
"quanto custa" (`CalculadoraPedido`), "quantos pontos" (`CalculadoraPontos`) e
"pode mudar de status?" (`StatusPedido`/`Pedido`) são classes de domínio puras,
sem acesso a banco, HTTP ou fila — por isso são testadas em isolamento (ver
`CalculadoraPedidoTest`, `CalculadoraPontosTest`, `PedidoStatusTest`).

A publicação de eventos é feita via a interface `PublicadorDeEventos`, para que
o `PedidoService` nunca dependa diretamente do RabbitMQ — trocar de broker (ou
usar um fake nos testes) significa trocar só a implementação.

### Por que dois bancos de dados

- **PostgreSQL** — núcleo transacional (cliente, produto, pedido, item_pedido).
  Precisa de consistência forte, chaves estrangeiras e transações ACID (reserva
  de estoque, criação do pedido).
- **MongoDB** — extrato/histórico de pontos de fidelidade. É um log de
  lançamentos, consultado por cliente, sem necessidade de joins relacionais —
  encaixa melhor num modelo de documentos.

### Diagrama de alto nível

```
Cliente HTTP
    │
    ▼
┌───────────────┐      valida / orquestra      ┌─────────────────────┐
│ PedidoController│ ───────────────────────────▶ │     PedidoService    │
└───────────────┘                               └──────────┬───────────┘
                                                             │
                       ┌─────────────────────────────────────┼───────────────────────┐
                       ▼                                     ▼                       ▼
             CalculadoraPedido /                    PostgreSQL (JPA)         PublicadorDeEventos
             CalculadoraPontos                    Cliente/Produto/Pedido            │
             (regras puras)                                                          ▼
                                                                              RabbitMQ (pedidos.exchange)
                                                                              routing key: pedido.pago
                                                                                       │
                                                                                       ▼
                                                                          PedidoPagoConsumidor (@RabbitListener)
                                                                                       │
                                                                                       ▼
                                                                              FidelidadeService
                                                                            (crédito idempotente)
                                                                                       │
                                                                                       ▼
                                                                          MongoDB: extrato_pontos
```

---

## Stack técnica

| Camada             | Tecnologia                                             |
|---------------------|---------------------------------------------------------|
| Linguagem/Framework  | Java 17, Spring Boot                                    |
| Web                  | Spring Web (REST), Bean Validation (`spring-boot-starter-validation`) |
| Persistência SQL     | Spring Data JPA + PostgreSQL                             |
| Versionamento de schema | Flyway (`db/migration`)                               |
| Persistência NoSQL   | Spring Data MongoDB (extrato de pontos)                   |
| Mensageria           | RabbitMQ (Spring AMQP), com fila de dead-letter (DLQ)      |
| Documentação da API  | springdoc-openapi (Swagger UI)                            |
| Empacotamento        | Maven (`mvnw`)                                             |
| Containers           | Docker + Docker Compose                                    |
| Testes               | JUnit 5 (Spring Boot Starter Test)                          |

---

## Modelo de domínio

- **Cliente** — `nome`, `tipo` (`COMUM` ou `PLUS`). Cliente `PLUS` tem desconto
  e pontuação diferenciados.
- **Produto** — `nome`, `categoria`, `preco`, `estoque`, com controle de
  concorrência otimista (`@Version`) para evitar condições de corrida na
  baixa de estoque.
- **Pedido** — dono da lista de `ItemPedido`, do `status` (máquina de
  estados) e do resumo financeiro (`subtotal`, `desconto`, `frete`, `total`,
  `pontosGerados`).
- **ItemPedido** — produto + quantidade + preço unitário **congelado** no
  momento da criação do pedido (alterações futuras no preço do produto não
  afetam pedidos já criados).
- **Cupom** (enum) — `DESC10` (10% de desconto) e `FRETEGRATIS` (zera o
  frete).
- **LancamentoDePontos** (Mongo) — um lançamento por pedido pago, com índice
  único em `pedidoId` para garantir idempotência do crédito.

---

## Regras de negócio

O cálculo financeiro (`CalculadoraPedido`) e de pontos (`CalculadoraPontos`)
são funções puras, sem efeitos colaterais. As constantes de negócio ficam
centralizadas em `RegrasDoPedido`:

1. **Subtotal** = soma de `precoUnitario × quantidade` de todos os itens.
2. **Desconto** = `(5% se cliente PLUS) + (percentual do cupom, se houver)`,
   aplicado sobre o subtotal. `DESC10` soma 10 p.p.; um cliente PLUS com
   `DESC10` acumula os dois (15%).
3. **Frete** = fixo em R$ 20,00; é **grátis** se o subtotal atingir R$ 200,00
   **ou** se o cupom aplicado isentar frete (`FRETEGRATIS`).
4. **Total** = `subtotal − desconto + frete`, nunca negativo (limitado a zero).
5. Todo valor monetário é normalizado para **2 casas decimais**, com
   arredondamento `HALF_UP`.
6. **Pontos de fidelidade** = 1 ponto para cada R$ 1,00 do total pago;
   cliente **PLUS** acumula o **dobro** de pontos. Pontos só são gerados no
   momento do **pagamento**, nunca na criação do pedido.
7. **Estoque** — a validação de estoque considera a **quantidade somada por
   produto** no pedido (o mesmo produto pode aparecer em mais de uma linha).
   Primeiro **todo** o pedido é validado; só depois o estoque é reservado —
   evita reservar parcialmente um pedido que vai falhar em outro item.
8. **Cancelamento** devolve ao estoque as quantidades reservadas de cada
   item do pedido.
9. Um pedido precisa ter **pelo menos 1 item**, e cada item precisa ter
   **quantidade > 0**.
10. Um **cupom inválido** (código que não existe) rejeita a criação do
    pedido.
11. O crédito de pontos é **idempotente**: se o evento `pedido-pago` for
    entregue mais de uma vez (reentrega do RabbitMQ), o índice único em
    `pedidoId` no Mongo impede duplicidade — o segundo crédito é
    silenciosamente ignorado.

---

## Máquina de estados do pedido

```
CRIADO ──pagar──▶ PAGO ──enviar──▶ ENVIADO ──entregar──▶ ENTREGUE (final)
  │                  │
  └───cancelar───▶ CANCELADO (final)   └───cancelar───▶ CANCELADO (final)
```

Transições permitidas (`StatusPedido`):

| De        | Para                    |
|-----------|--------------------------|
| CRIADO    | PAGO, CANCELADO          |
| PAGO      | ENVIADO, CANCELADO       |
| ENVIADO   | ENTREGUE                 |
| ENTREGUE  | *(nenhuma — final)*      |
| CANCELADO | *(nenhuma — final)*      |

Qualquer transição fora dessa tabela lança `TransicaoDeStatusInvalidaException`
(HTTP 409).

---

## Fluxo assíncrono de fidelidade

1. `POST /pedidos/{id}/pagamento` chama `PedidoService.pagar`, que calcula os
   pontos, muda o status para `PAGO` e persiste o pedido — tudo dentro da
   mesma transação.
2. Após o commit, o serviço publica o evento `PedidoPagoEvento` na exchange
   `pedidos.exchange`, routing key `pedido.pago`.
3. A fila `pedidos.pedido-pago` está ligada a essa exchange e configurada
   com **dead-letter exchange** (`pedidos.dlx` → fila `pedidos.pedido-pago.dlq`),
   com até 3 tentativas de reprocessamento antes de cair na DLQ.
4. `PedidoPagoConsumidor` escuta a fila e delega para `FidelidadeService`, que
   grava um `LancamentoDePontos` no MongoDB, chaveado por `pedidoId` — reentregas
   duplicadas do RabbitMQ não geram pontos duplicados.
5. `GET /clientes/{id}/pontos` lê o extrato consolidado (saldo + histórico) do
   MongoDB.

---

## Endpoints da API

Documentação interativa (Swagger UI) disponível em `/swagger-ui.html` com a
aplicação rodando.

| Método | Rota                          | Descrição                                                  |
|--------|--------------------------------|--------------------------------------------------------------|
| POST   | `/pedidos`                     | Cria um pedido: valida entrada/estoque, reserva estoque e calcula os valores |
| GET    | `/pedidos/{id}`                | Consulta um pedido pelo id                                    |
| GET    | `/pedidos?clienteId=&status=`  | Lista pedidos, com filtro opcional por cliente e/ou status     |
| POST   | `/pedidos/{id}/pagamento`      | Paga o pedido: gera pontos e publica o evento `pedido-pago`    |
| POST   | `/pedidos/{id}/cancelamento`   | Cancela o pedido e devolve as quantidades ao estoque            |
| GET    | `/produtos`                    | Lista o catálogo com o estoque atual                            |
| GET    | `/clientes/{id}/pontos`        | Extrato de pontos do cliente (saldo + lançamentos, via MongoDB) |

### Exemplo — criar um pedido

```bash
curl -X POST http://localhost:8080/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": 1,
    "itens": [
      { "produtoId": 1, "quantidade": 2 },
      { "produtoId": 3, "quantidade": 1 }
    ],
    "cupom": "DESC10"
  }'
```

### Exemplo — pagar um pedido

```bash
curl -X POST http://localhost:8080/pedidos/1/pagamento
```

A carga inicial (`V2__carga_inicial_do_catalogo.sql`) já cadastra produtos
(Camiseta, Tênis, Fone, Geladeira, Micro-ondas) e clientes de exemplo (COMUM e
PLUS) para facilitar testes manuais.

---

## Tratamento de erros

O `ApiExceptionHandler` centraliza a tradução de exceções de domínio para
respostas HTTP padronizadas (corpo com `status`, `erro`, `mensagem` e `path`):

| Exceção                                | HTTP | Situação                                                     |
|-----------------------------------------|------|----------------------------------------------------------------|
| `RecursoNaoEncontradoException`          | 404  | Cliente, produto ou pedido não encontrado                       |
| `TransicaoDeStatusInvalidaException`     | 409  | Tentativa de mudança de status não permitida                    |
| `RegraDeNegocioException` (e subclasses: `EstoqueInsuficienteException`, `CupomInvalidoException`) | 422  | Estoque insuficiente, cupom inválido, pedido sem itens, etc. |
| `MethodArgumentNotValidException`        | 400  | Payload inválido segundo o Bean Validation, com a lista de campos inválidos |
| Qualquer outra exceção                   | 500  | Erro interno inesperado (logado no servidor)                     |

---

## Como rodar

### Opção 1 — tudo em containers (recomendado)

Pré-requisitos: Docker e Docker Compose.

```bash
docker compose up --build
```

Isso sobe PostgreSQL, MongoDB, RabbitMQ e a aplicação (que só inicia depois
que as dependências passam no healthcheck). As migrações do Flyway rodam
automaticamente no startup.

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Console de administração do RabbitMQ: http://localhost:15672 (usuário/senha: `pedidos`/`pedidos`)

Para parar e remover os containers:

```bash
docker compose down
```

Para também apagar os volumes (dados de Postgres/Mongo):

```bash
docker compose down -v
```

### Opção 2 — infraestrutura em container, app local (útil para debugar na IDE)

```bash
docker compose up -d postgres mongo rabbitmq
./mvnw spring-boot:run
```

Nesse modo a aplicação usa os valores padrão do `application.yaml`
(`localhost` nas portas expostas pelo `docker-compose.yml`), então não é
necessário configurar nenhuma variável de ambiente.

### Opção 3 — tudo local, sem Docker

Suba manualmente um PostgreSQL, um MongoDB e um RabbitMQ acessíveis em
`localhost` (ou ajuste as variáveis de ambiente abaixo para apontar para onde
estiverem rodando) e então:

```bash
./mvnw spring-boot:run
```

---

## Variáveis de ambiente

Todas têm valor padrão (usado nas opções 2 e 3 acima), então nada precisa ser
configurado ao usar `docker compose up` (o `docker-compose.yml` já injeta os
valores corretos para o app enxergar os outros serviços pelo nome do
container).

| Variável                     | Padrão                                                              | Descrição                        |
|--------------------------------|------------------------------------------------------------------------|-------------------------------------|
| `SPRING_DATASOURCE_URL`        | `jdbc:postgresql://localhost:5432/pedidos`                             | URL de conexão do PostgreSQL          |
| `SPRING_DATASOURCE_USERNAME`   | `pedidos`                                                               | Usuário do PostgreSQL                 |
| `SPRING_DATASOURCE_PASSWORD`   | `pedidos`                                                               | Senha do PostgreSQL                   |
| `SPRING_MONGODB_URI`           | `mongodb://pedidos:pedidos@localhost:27017/pedidos?authSource=admin`   | URI de conexão do MongoDB             |
| `SPRING_RABBITMQ_HOST`         | `localhost`                                                             | Host do RabbitMQ                      |
| `SPRING_RABBITMQ_PORT`         | `5672`                                                                  | Porta AMQP do RabbitMQ                |
| `SPRING_RABBITMQ_USERNAME`     | `pedidos`                                                               | Usuário do RabbitMQ                   |
| `SPRING_RABBITMQ_PASSWORD`     | `pedidos`                                                               | Senha do RabbitMQ                     |
| `SERVER_PORT`                  | `8080`                                                                  | Porta HTTP da aplicação               |

---

## Testes

```bash
./mvnw test
```

A suíte cobre principalmente as regras de negócio puras e a máquina de
estados, sem precisar subir o contexto do Spring nem infraestrutura externa:

- `CalculadoraPedidoTest` — subtotal, desconto (PLUS/cupom/acumulado), frete
  grátis por valor mínimo ou cupom, total nunca negativo.
- `CalculadoraPontosTest` — pontuação base e em dobro para PLUS.
- `PedidoStatusTest` — todas as transições permitidas e proibidas.
- `PedidoServiceTest` — criação (validação de estoque somado por produto,
  cupom inválido, pedido sem itens), pagamento (geração de pontos e
  publicação de evento) e cancelamento (devolução ao estoque).
- `FidelidadeServiceTest` — idempotência do crédito de pontos.
- `ProdutoTest`, `ClienteTest`, `CupomTest` — invariantes das entidades.

---

## Estrutura de pastas

```
pedidos/
├── src/
│   ├── main/
│   │   ├── java/com/penambucanas/pedidos/
│   │   │   ├── api/            # Controllers, DTOs, exception handler
│   │   │   ├── domain/          # Entidades e enums
│   │   │   │   └── regras/       # Regras de cálculo puras (sem Spring)
│   │   │   ├── exception/       # Exceções de domínio
│   │   │   ├── fidelidade/      # Módulo de pontos (Mongo)
│   │   │   ├── messaging/       # Contrato de eventos + implementação RabbitMQ
│   │   │   ├── repository/      # Repositórios Spring Data JPA
│   │   │   └── service/         # Casos de uso / orquestração
│   │   └── resources/
│   │       ├── application.yaml
│   │       └── db/migration/    # Scripts Flyway (schema + carga inicial)
│   └── test/java/...            # Testes unitários
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```
