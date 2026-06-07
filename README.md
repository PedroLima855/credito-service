# 💳 Crédito Service

Microserviço de gestão de créditos para parceiros em uma plataforma B2B. Projetado para alta concorrência, confiabilidade e rastreabilidade de operações financeiras.

---

## 📋 Índice

- [Visão Geral](#-visão-geral)
- [Tecnologias](#-tecnologias)
- [Arquitetura](#-arquitetura)
- [Como Executar](#-como-executar)
- [Documentação Swagger](#-documentação-swagger)
- [Endpoints da API](#-endpoints-da-api)
- [Modelo de Dados](#-modelo-de-dados)
- [Funcionalidades](#-funcionalidades)
- [Testes](#-testes)
- [Estrutura do Projeto](#-estrutura-do-projeto)

---

## 🎯 Visão Geral

Sistema crítico para operação B2B que permite a milhares de parceiros simultaneamente:

- **Reportar vendas** → adição de créditos
- **Consumir créditos** → débitos com validação de saldo
- **Consultar saldo** → em tempo real com cache
- **Rastrear operações** → histórico completo de transações

---

## 🛠 Tecnologias

| Tecnologia | Função |
|---|---|
| **Kotlin 1.9** | Linguagem principal |
| **Spring Boot 3.3** | Framework base |
| **Spring Data JPA** | Persistência |
| **PostgreSQL 16** | Banco de dados |
| **Flyway** | Versionamento de schema |
| **RabbitMQ 3** | Mensageria / Notificações |
| **Caffeine** | Cache em memória |
| **Spring Retry** | Mecanismo de retry |
| **SpringDoc OpenAPI** | Documentação Swagger |
| **Docker Compose** | Orquestração de containers |
| **JUnit 5 + Mockito** | Testes |

---

## 🏗 Arquitetura

```
┌─────────────┐       ┌──────────────────┐       ┌────────────┐
│   Cliente   │──────▶│  Crédito Service │──────▶│ PostgreSQL │
└─────────────┘       │                  │       └────────────┘
                      │  ┌────────────┐  │
                      │  │  Caffeine  │  │       ┌────────────┐
                      │  │   Cache    │  │──────▶│  RabbitMQ  │
                      │  └────────────┘  │       └────────────┘
                      └──────────────────┘
```

### Camadas

```
controller/     → Endpoints REST + tratamento de erros
service/        → Regras de negócio + cache + retry
repository/     → Acesso a dados + lock pessimista
entity/         → Entidades JPA
dto/            → Objetos de request/response
config/         → Configurações (RabbitMQ, OpenAPI)
```

---

## 🚀 Como Executar

### Com Docker Compose (recomendado)

Um único comando sobe toda a infraestrutura:

```bash
docker-compose up --build
```

Isso inicia:
| Serviço | Porta | Credenciais |
|---|---|---|
| API | `localhost:8080` | — |
| PostgreSQL | `localhost:5432` | postgres / postgres |
| RabbitMQ | `localhost:5672` | guest / guest |
| RabbitMQ UI | `localhost:15672` | guest / guest |

### Localmente (desenvolvimento)

Pré-requisitos: PostgreSQL e RabbitMQ rodando em localhost.

```bash
# Criar o banco
createdb gest_dados

# Rodar a aplicação
./mvnw spring-boot:run
```

O Flyway cria as tabelas automaticamente no startup.

---

## 📖 Documentação Swagger

Com a aplicação rodando, acesse a documentação interativa da API:

| Recurso | URL |
|---|---|
| **Swagger UI** | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) |
| **OpenAPI JSON** | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) |

A interface do Swagger permite:
- 📝 Visualizar todos os endpoints documentados
- ▶️ Testar requisições diretamente no navegador
- 📋 Ver schemas de request/response
- 🔍 Explorar os códigos de status e descrições

---

## 📡 Endpoints da API

Base URL: `http://localhost:8080/api/creditos`

### Consultar Saldo

```http
GET /saldo/{parceiroId}
```

**Response 200:**
```json
{
  "parceiroId": "parceiro-001",
  "saldo": 750.00
}
```

---

### Adicionar Crédito

```http
POST /adicionar
Content-Type: application/json
```

**Request:**
```json
{
  "parceiroId": "parceiro-001",
  "valor": 1000.00,
  "descricao": "Venda realizada - Lote 01",
  "chaveIdempotencia": "venda-001"
}
```

**Response 201:**
```json
{
  "id": "a1b2c3d4-...",
  "parceiroId": "parceiro-001",
  "tipo": "CREDITO",
  "valor": 1000.00,
  "descricao": "Venda realizada - Lote 01",
  "status": "CONCLUIDA",
  "criadoEm": "2024-01-15T10:30:00"
}
```

---

### Consumir Crédito

```http
POST /consumir
Content-Type: application/json
```

**Request:**
```json
{
  "parceiroId": "parceiro-001",
  "valor": 300.00,
  "descricao": "Consumo de serviço premium",
  "chaveIdempotencia": "consumo-001"
}
```

**Response 201** (sucesso) ou **422** (saldo insuficiente):
```json
{
  "erro": "Saldo insuficiente. Disponível: R$200.00, Solicitado: R$300.00",
  "timestamp": "2024-01-15T10:35:00"
}
```

---

### Histórico de Transações

```http
GET /historico/{parceiroId}
```

**Response 200:**
```json
[
  {
    "id": "a1b2c3d4-...",
    "parceiroId": "parceiro-001",
    "tipo": "CREDITO",
    "valor": 1000.00,
    "descricao": "Venda realizada",
    "status": "CONCLUIDA",
    "criadoEm": "2024-01-15T10:30:00"
  },
  {
    "id": "e5f6g7h8-...",
    "parceiroId": "parceiro-001",
    "tipo": "DEBITO",
    "valor": 300.00,
    "descricao": "Consumo de serviço",
    "status": "CONCLUIDA",
    "criadoEm": "2024-01-15T10:35:00"
  }
]
```

---

## 🗃 Modelo de Dados

### Tabela: `transacoes`

| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | UUID (PK) | Identificador único |
| `parceiro_id` | VARCHAR(100) | ID do parceiro |
| `tipo` | VARCHAR(10) | `CREDITO` ou `DEBITO` |
| `valor` | NUMERIC(15,2) | Valor da transação (> 0) |
| `descricao` | VARCHAR(500) | Motivo/descrição |
| `status` | VARCHAR(20) | `PENDENTE`, `CONCLUIDA` ou `FALHA` |
| `chave_idempotencia` | VARCHAR(255) | Chave única para evitar duplicidade |
| `criado_em` | TIMESTAMP | Data/hora de criação |
| `atualizado_em` | TIMESTAMP | Data/hora da última atualização |

**Índices:** `parceiro_id`, `status`, `criado_em`

---

## ⚙️ Funcionalidades

### 🔒 Controle de Concorrência

Débitos utilizam `SELECT ... FOR UPDATE` (lock pessimista no PostgreSQL), garantindo que requisições simultâneas do mesmo parceiro não causem race conditions nem saldos negativos.

### 🧠 Cache (Caffeine)

- Consulta de saldo cacheada por **10 segundos** com máximo de **1000 entradas**
- Cache invalidado automaticamente a cada crédito ou débito (`@CacheEvict`)

### 🔄 Idempotência

Toda transação pode incluir uma `chaveIdempotencia`. Se uma requisição duplicada for enviada com a mesma chave, o sistema retorna a transação original sem processar novamente.

### 🔁 Retry Automático

| Operação | Tentativas | Backoff |
|---|---|---|
| Notificação (RabbitMQ) | 3x | Exponencial (1s → 2s → 4s) |
| Conciliação | 3x | Fixo (2s) |

Após esgotar as tentativas, o `@Recover` registra a falha em log sem derrubar a aplicação.

### 📨 Notificações (Mensageria)

Transações concluídas são publicadas na fila `transacoes.notificacoes` do RabbitMQ. Consumidores externos podem reagir a eventos como:
- Créditos de alto valor
- Padrões de consumo
- Alertas operacionais

### 🔧 Conciliação Automática

Um scheduler executa a cada **60 segundos** e resolve transações que ficaram com status `PENDENTE` há mais de **5 minutos** (simulando problemas de comunicação resolvidos).

### 🛡 Tratamento de Erros

| HTTP Status | Cenário |
|---|---|
| 200 | Consulta bem-sucedida |
| 201 | Transação criada |
| 400 | Validação falhou (campos obrigatórios, valores inválidos) |
| 422 | Saldo insuficiente |
| 500 | Erro interno |

### 🏊 Connection Pool (HikariCP)

Configurado para alta concorrência:
- **20** conexões máximas
- **5** conexões idle mínimas
- **5s** timeout de conexão

---

## 🧪 Testes

### Executar

```bash
./mvnw test
```

### Cobertura (25 testes)

| Classe de Teste | Qtd | Tipo |
|---|---|---|
| `CreditoServiceTest` | 12 | Unitário (mocks) |
| `CreditoControllerTest` | 10 | Integração (MockMvc) |
| `NotificacaoServiceTest` | 2 | Unitário (mocks) |
| `CreditoServiceApplicationTests` | 1 | Context load |

### Cenários Cobertos

✅ Consulta de saldo (com e sem transações)  
✅ Adição de crédito  
✅ Consumo com saldo suficiente  
✅ Consumo com saldo insuficiente (422)  
✅ Idempotência (crédito e débito)  
✅ Histórico (com e sem dados)  
✅ Conciliação de transações pendentes  
✅ Validação de campos obrigatórios (400)  
✅ Envio de notificação para RabbitMQ  
✅ Propagação de erro para retry  

---

## 📁 Estrutura do Projeto

```
credito-service/
├── docker-compose.yml
├── Dockerfile
├── pom.xml
└── src/
    ├── main/
    │   ├── kotlin/com/credito/
    │   │   ├── CreditoServiceApplication.kt
    │   │   ├── config/
    │   │   │   ├── OpenApiConfig.kt
    │   │   │   └── RabbitConfig.kt
    │   │   ├── controller/
    │   │   │   ├── CreditoController.kt
    │   │   │   └── GlobalExceptionHandler.kt
    │   │   ├── dto/
    │   │   │   └── Dtos.kt
    │   │   ├── entity/
    │   │   │   └── Transacao.kt
    │   │   ├── repository/
    │   │   │   └── TransacaoRepository.kt
    │   │   └── service/
    │   │       ├── CreditoService.kt
    │   │       └── NotificacaoService.kt
    │   └── resources/
    │       ├── application.properties
    │       └── db/migration/
    │           └── V1__criar_tabela_transacoes.sql
    └── test/
        ├── kotlin/com/credito/
        │   ├── CreditoServiceApplicationTests.kt
        │   ├── controller/
        │   │   └── CreditoControllerTest.kt
        │   └── service/
        │       ├── CreditoServiceTest.kt
        │       └── NotificacaoServiceTest.kt
        └── resources/
            └── application-test.properties
```

---

## 📄 Licença

Este projeto é de uso livre para fins de estudo e desenvolvimento.
