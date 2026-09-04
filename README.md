# Banking API RESTful

API RESTful para gerenciamento de transações bancárias, construída com **Spring Boot 3.3**, **Spring Security + JWT**, **Spring Data JPA** e **Flyway** para migrações de banco de dados.

## Tecnologias

| Tecnologia | Versão | Descrição |
|---|---|---|
| Java | 21 | Linguagem base |
| Spring Boot | 3.3.x | Framework principal |
| Spring Security | 6.x | Autenticação e autorização |
| JWT (Nimbus JOSE+JWT) | 9.x | Token-based authentication |
| Spring Data JPA | 3.x | Persistência com Hibernate |
| Flyway | 10.x | Versionamento de banco de dados |
| MySQL | 8.x | Banco de dados |
| H2 | 2.x | Banco em memória para testes |
| Maven | 3.9.x | Build e dependências |

## Arquitetura

```
com.ricardosenna.bankingapi
├── config          → SecurityConfig (Spring Security + JWT)
├── controller      → REST endpoints (Clients, Accounts, Transactions, Auth)
├── dto             → Request/Response records com validação (@Valid)
├── entity          → JPA entities (User, Client, Account, Transaction)
├── enums           → Enums de domínio (AccountType, TransactionType, etc.)
├── exception       → GlobalExceptionHandler + custom exceptions
├── repository      → Spring Data JPA interfaces
├── security        → JwtService, JwtAuthenticationFilter
├── service         → BankingService (regras de negócio), AuthService
```

## Endpoints

### Autenticação

| Method | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/api/auth/register` | Registrar novo usuário |
| `POST` | `/api/auth/login` | Login e obtenção de token JWT |
| `POST` | `/api/auth/refresh` | Renovar access token usando refresh token |
| `POST` | `/api/auth/logout` | Revogar refresh token |

### Documentação interativa

Com a aplicação em execução, a documentação está disponível em [`/swagger-ui.html`](http://localhost:8080/swagger-ui.html) e o contrato OpenAPI em [`/v3/api-docs`](http://localhost:8080/v3/api-docs). A interface possui o botão **Authorize** para informar o Bearer JWT.

### Clientes

| Method | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/api/clients` | Criar cliente |
| `GET` | `/api/clients/{cpf}` | Buscar cliente por CPF |
| `GET` | `/api/clients` | Listar todos os clientes (paginado) |
| `PUT` | `/api/clients/{cpf}` | Atualizar nome e email do cliente |
| `PATCH` | `/api/clients/{cpf}/status` | Alterar status (ACTIVE/BLOCKED) |

### Contas

| Method | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/api/accounts` | Criar conta (CHECKING ou SAVINGS) |
| `GET` | `/api/accounts/{number}` | Buscar conta por número |
| `GET` | `/api/accounts` | Listar contas por clientId |
| `PATCH` | `/api/accounts/{number}/status` | Bloquear ou ativar conta |
| `DELETE` | `/api/accounts/{number}` | Encerrar conta com saldo zero (exclusão lógica) |

### Transações

| Method | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/api/transactions/deposit` | Depositar valor |
| `POST` | `/api/transactions/withdraw` | Sacar valor |
| `POST` | `/api/transactions/transfer` | Transferir entre contas |
| `GET` | `/api/transactions/accounts/{number}/statement` | Extrato paginado e filtrável por `type`, `startDate` e `endDate` |

### Usuário autenticado

| Method | Endpoint | Descrição |
|--------|----------|-----------|
| `PUT` | `/api/users/me` | Atualizar nome e email do usuário autenticado |
| `PATCH` | `/api/users/me/password` | Alterar senha informando a senha atual |

## Como Executar

### Pré-requisitos
- Java 21+
- Maven 3.9+
- MySQL 8.x (ou Docker)

### 1. Configurar banco de dados

**Opção A — Docker (recomendado):**
```bash
docker-compose up -d
```

**Opção B — MySQL local:**
```sql
CREATE DATABASE banking_api CHARACTER SET utf8mb4;
```

### 2. Configurar variáveis de ambiente
```bash
export DB_URL="jdbc:mysql://localhost:3306/banking_api"
export DB_USERNAME="root"
export DB_PASSWORD="secret"
export JWT_SECRET="$(openssl rand -base64 32)"
```

### 3. Build e execução
```bash
mvn clean install
mvn spring-boot:run
```

A API estará disponível em `http://localhost:8080`

## Exemplos de Uso

### 1. Registrar usuário
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name": "Ricardo Senna", "email": "ricardo@example.com", "password": "SenhaSegura123"}'
```

### 2. Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "ricardo@example.com", "password": "SenhaSegura123"}'
```

### 3. Criar cliente (requer token)
```bash
curl -X POST http://localhost:8080/api/clients \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"cpf": "12345678901", "name": "João Silva", "email": "joao@example.com"}'
```

### 4. Criar conta
```bash
curl -X POST http://localhost:8080/api/accounts \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"cpf": "12345678901", "type": "CHECKING", "withdrawFee": "2.00", "interestRate": null}'
```

### 5. Depositar
```bash
curl -X POST http://localhost:8080/api/transactions/deposit \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"accountNumber": 1001, "amount": "500.00"}'
```

### Ownership e autorização

Cada cliente criado por um usuário autenticado é vinculado ao seu usuário (`User → Client → Account`). Usuários comuns só podem consultar e operar suas próprias contas; a conta de destino de uma transferência pode pertencer a outro cliente, mas a conta de origem sempre precisa pertencer ao usuário autenticado. Usuários `ADMIN` podem consultar clientes e contas de qualquer usuário e executar operações administrativas.

O endpoint `POST /api/clients` deve ser chamado autenticado e um usuário só pode possuir um perfil de cliente. A migration `V6__link_clients_to_users.sql` mantém `user_id` anulável para permitir migração gradual de dados antigos.

### Recursos operacionais

- **Rate limiting:** login é limitado a 5 requisições por minuto por IP; operações em `/api/transactions/**` são limitadas a 30 por minuto por IP. O excesso retorna `429` e `Retry-After: 60`.
- **CORS:** a origem padrão de desenvolvimento é `http://localhost:5173`; configure `CORS_ALLOWED_ORIGINS` com uma lista separada por vírgulas em outros ambientes.
- **Logs:** o Logback emite eventos em JSON e não registra senhas nem JWTs completos.
- **Cache:** consultas de cliente podem ser cacheadas e o cache é invalidado em atualizações. Saldos e extratos não são cacheados para evitar dados financeiros obsoletos.
- **Redis:** o `docker-compose.yml` inclui Redis. O padrão usa cache local para facilitar os testes; defina `CACHE_TYPE=redis`, `REDIS_HOST` e `REDIS_PORT` para ativar cache distribuído.
- **Refresh token:** refresh tokens são armazenados apenas como hash, expiram em 7 dias por padrão e são revogados no logout e na troca de senha. Configure `JWT_REFRESH_EXPIRATION_SECONDS` quando necessário.

## Tratamento de Erros

A API retorna erros padronizados:

```json
{
  "timestamp": "2026-08-20T15:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "There are invalid fields in the request",
  "path": "/api/clients",
  "fieldErrors": {
    "cpf": "CPF must contain exactly 11 digits"
  }
}
```

| Status | Significado |
|--------|-------------|
| 201 | Criado com sucesso |
| 400 | Erro de validação ou regra de negócio |
| 401 | Token inválido ou ausente |
| 403 | Acesso negado |
| 404 | Recurso não encontrado |
| 409 | Conflito (CPF duplicado) |
| 500 | Erro interno |

## Testes

```bash
mvn test
```

Os testes usam H2 em memória e cobrem:
- Integração completa da service layer
- Autenticação JWT (geração, validação, rejeição)
- Endpoints REST (MockMvc)
- Casos negativos (saldo insuficiente, CPF duplicado, cliente bloqueado)

## Migrations Flyway

| Version | Descrição |
|---------|-----------|
| V1 | Cria tabela `users` (autenticação) |
| V2 | Cria tabela `clients` |
| V3 | Cria tabela `accounts` |
| V4 | Cria tabela `transactions` |

## Próximos Passos (Spring Boot Avançado)

- [ ] Adicionar cache com `@Cacheable` (Redis)
- [ ] Implementar Rate Limiting
- [ ] Adicionar logs estruturados (JSON)
- [ ] Configurar CORS para frontend
- [ ] Implementar refresh token
- [ ] Adicionar Swagger/OpenAPI documentation
- [ ] Deploy com Docker Compose completo

## Licença

MIT License — ver arquivo `LICENSE`
