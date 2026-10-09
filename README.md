# User Management API

API REST de gerenciamento de usuários e endereços, desenvolvida como parte de um teste técnico para a vaga de Analista de Sistemas Java Pleno.

Cada usuário possui um ou mais endereços, validados pela API pública do [ViaCEP](https://viacep.com.br). A API inclui autenticação e autorização por perfil, paginação, auditoria, exclusão lógica e tratamento padronizado de erros.

> O frontend (Angular) fica em um repositório separado: _link a adicionar_.

## Status do projeto

- [x] Estrutura do projeto, perfis de configuração, migration e entidades
- [x] Autenticação com JWT (login, emissão de token, rotas protegidas) e criação do ADMIN inicial
- [x] Autorização por perfil nos endpoints (ADMIN e USER)
- [x] CRUD de usuários e endereços (criar, buscar por id, atualizar e excluir)
- [x] Listagem com paginação, filtros e ordenação
- [x] Integração com o ViaCEP, com validação de CEP e cache
- [x] Tratamento de erros (Problem Details) e documentação OpenAPI
- [ ] Testes e relatório de cobertura
- [ ] Docker e Docker Compose

> Este README é atualizado conforme o desenvolvimento avança.

## Stack

| Camada | Tecnologias |
|---|---|
| Linguagem e framework | Java 21, Spring Boot 4.1.1 |
| Persistência | Spring Data JPA, Hibernate, Flyway |
| Segurança | Spring Security, JWT, BCrypt |
| Validação e mapeamento | Bean Validation, MapStruct, Lombok |
| Integração externa | RestClient (ViaCEP), cache com Caffeine |
| Banco de dados | PostgreSQL (produção), H2 em modo PostgreSQL (desenvolvimento e testes) |
| Documentação | OpenAPI / Swagger UI (springdoc) |
| Infraestrutura | Docker e Docker Compose |

## Estrutura do projeto

```
src/main/java/br/com/eric/usermanagement/
├── config         # Auditoria, cache, OpenAPI
├── controller
├── service
├── repository
├── domain         # Entidades e enums
├── dto
├── mapper
├── client         # Integração com o ViaCEP
├── security
└── exception
src/main/resources/
├── application.yml
├── application-dev.yml
├── application-prod.yml
└── db/migration   # Migrations do Flyway
```

## Como executar

### Pré-requisitos

- JDK 21
- Maven (ou o wrapper `mvnw` incluído no projeto)

### Modo de desenvolvimento (H2)

```bash
./mvnw spring-boot:run
```

No Windows (PowerShell):

```powershell
.\mvnw.cmd spring-boot:run
```

O perfil `dev` é o padrão. A aplicação sobe na porta `8080` com banco H2 em memória, e as migrations do Flyway criam as tabelas automaticamente.

- Swagger UI: http://localhost:8080/swagger-ui.html (faça o login, copie o `accessToken` e use o botão **Authorize**)
- Console do H2: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:usersdb`, usuário `sa`, sem senha)

### Perfil de produção (PostgreSQL)

O perfil `prod` lê as credenciais por variáveis de ambiente, sem valores fixos no código:

| Variável | Obrigatória | Descrição |
|---|---|---|
| `DB_URL` | Sim | URL JDBC do PostgreSQL |
| `DB_USER` | Sim | Usuário do banco |
| `DB_PASSWORD` | Sim | Senha do banco |
| `JWT_SECRET` | Sim | Segredo de assinatura do JWT (mínimo de 32 caracteres) |
| `JWT_EXPIRATION_MINUTES` | Não | Validade do token em minutos (padrão: 60) |
| `ADMIN_EMAIL` | Sim | E-mail do ADMIN criado na primeira subida |
| `ADMIN_PASSWORD` | Sim | Senha do ADMIN criado na primeira subida |
| `ADMIN_NAME` | Não | Nome do ADMIN inicial (padrão: Administrador) |
| `SWAGGER_ENABLED` | Não | Habilita o Swagger UI e o `/v3/api-docs` (padrão: `true`) |
| `APP_VIACEP_BASE_URL` | Não | URL base do ViaCEP (padrão: `https://viacep.com.br/ws`) |

A aplicação não sobe se `JWT_SECRET` tiver menos de 32 caracteres.

```bash
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

### Docker

_Será documentado após a conclusão do Docker Compose._

### Testes

_Será documentado após a implementação dos testes._

## Autenticação

A API é stateless e usa JWT (assinado com HS256). O endpoint de login é público, e os demais exigem o token no header `Authorization`.

**Usuário inicial (apenas perfil `dev`):**

| E-mail | Senha | Perfil |
|---|---|---|
| `admin@local.dev` | `Admin@123` | ADMIN |

Em produção, o ADMIN inicial é criado com `ADMIN_EMAIL` e `ADMIN_PASSWORD`. A criação só acontece se o e-mail ainda não existir.

**Login:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@local.dev","password":"Admin@123"}'
```

Resposta:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

**Uso do token:**

```bash
curl http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer <jwt>"
```

| Situação | Resposta |
|---|---|
| Sem token ou token inválido/expirado | `401` |
| Credenciais inválidas ou usuário inativo no login | `401` |
| Token válido sem permissão para o recurso | `403` |

O token carrega o e-mail (`sub`), o perfil (`role`) e o id do usuário (`userId`). Ele é assinado, mas não criptografado, e por isso não contém dados sensíveis.

## Endpoints

Base: `/api/v1`. A documentação interativa fica no Swagger UI.

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/auth/login` | Público | Autentica e devolve o token JWT |
| `POST` | `/users` | ADMIN | Cria usuário com um ou mais endereços (`201` + `Location`) |
| `GET` | `/users` | ADMIN | Lista usuários com paginação, filtros e ordenação |
| `GET` | `/users/{id}` | ADMIN ou o próprio usuário | Busca usuário com seus endereços |
| `PUT` | `/users/{id}` | ADMIN ou o próprio usuário | Atualiza usuário e endereços |
| `DELETE` | `/users/{id}` | ADMIN | Exclusão lógica do usuário e dos endereços (`204`) |

### Listagem

`GET /api/v1/users` (somente ADMIN) devolve uma página com um resumo de cada usuário e a cidade e o estado do **endereço principal**.

| Parâmetro | Descrição |
|---|---|
| `search` | Texto parcial no nome ou no e-mail |
| `city` | Texto parcial na cidade do endereço principal |
| `status` | `ACTIVE` ou `INACTIVE` |
| `role` | `ADMIN` ou `USER` |
| `page` | Número da página, começando em 0 (padrão: 0) |
| `size` | Itens por página (padrão: 10, máximo: 100) |
| `sort` | Campo e direção, por exemplo `city,desc`. Campos aceitos: `name`, `email`, `status`, `role`, `createdAt`, `city` |

Exemplo: `GET /api/v1/users?search=maria&status=ACTIVE&sort=city,desc&page=0&size=10`

Resposta:

```json
{
  "content": [
    { "id": 2, "name": "Maria Silva", "email": "maria@teste.com", "phone": "11999990000",
      "role": "USER", "status": "ACTIVE", "city": "São Paulo", "state": "SP" }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

A busca ignora maiúsculas e minúsculas, mas não ignora acentos. Campos de ordenação fora da lista retornam `400`.

### Validação de CEP (ViaCEP)

Na criação e na edição, o CEP de cada endereço é consultado no ViaCEP:

- CEP com formato inválido (diferente de 8 dígitos): `400`.
- CEP inexistente: `422`.
- ViaCEP indisponível (timeout ou erro): `503`. Nesse caso a operação não é concluída.
- O ViaCEP é a fonte da verdade para **estado e cidade**, que sobrescrevem os valores enviados. Rua e bairro também são sobrescritos quando o ViaCEP os informa.
- A consulta fica isolada na interface `CepClient`, implementada por `ViaCepClient`, o que facilita o uso de mocks nos testes.
- As respostas ficam em cache (Caffeine, até 1000 CEPs por 24 horas), inclusive os CEPs não encontrados. Falhas de comunicação não são guardadas em cache.
- Timeouts: 2 segundos para conectar e 3 segundos para ler.


### Regras de negócio

- **Perfis:** ADMIN tem acesso completo. USER visualiza e edita apenas os próprios dados e não pode alterar o próprio perfil (`role`) nem o `status`.
- **Endereços:** um usuário pode ter vários, mas apenas um é o principal. Se nenhum for marcado, o primeiro vira principal. Mais de um marcado é rejeitado.
- **Atualização de endereços:** com `id`, atualiza o existente. Sem `id`, cria um novo. Os endereços omitidos da lista são excluídos logicamente.
- **CEP:** validado no ViaCEP a cada criação ou edição de endereço (ver seção acima).
- **E-mail:** único e gravado em minúsculas. Permanece reservado mesmo após a exclusão lógica do usuário. Não pode ser alterado depois do cadastro.
- **Exclusão lógica:** nada é removido do banco. Registros excluídos deixam de aparecer nas consultas.
- **Auditoria:** `createdAt`, `updatedAt`, `createdBy` e `updatedBy` são preenchidos automaticamente, e o usuário registrado é o e-mail do token.
- **Senha:** armazenada com hash BCrypt e nunca devolvida nas respostas.

### Códigos de resposta

| Código | Quando |
|---|---|
| `200` / `201` / `204` | Sucesso (consulta e atualização / criação / exclusão) |
| `400` | Dados inválidos (validação dos campos) ou parâmetro de consulta inválido (filtro, página ou ordenação) |
| `401` | Token ausente, inválido ou expirado, ou credenciais inválidas no login |
| `403` | Usuário autenticado sem permissão para o recurso ou a alteração |
| `404` | Usuário não encontrado (inclusive excluído) |
| `409` | E-mail já cadastrado |
| `422` | Regra de negócio violada (ex.: mais de um endereço principal ou CEP inexistente) |
| `503` | ViaCEP indisponível no momento da validação do CEP |

### Formato dos erros

Todos os erros seguem o padrão Problem Details ([RFC 7807](https://www.rfc-editor.org/rfc/rfc7807)), com `Content-Type: application/problem+json`. Isso inclui os `401` e `403` gerados pelo Spring Security.

```json
{
  "title": "Dados inválidos",
  "status": 400,
  "detail": "Um ou mais campos são inválidos.",
  "instance": "/api/v1/users",
  "timestamp": "2026-10-09T14:30:00Z",
  "errors": [
    { "field": "addresses[0].cep", "message": "CEP deve conter 8 dígitos" }
  ]
}
```

Quando o campo `type` não aparece, vale o padrão `about:blank` da RFC. O campo `errors` aparece apenas nos erros de validação. Erros inesperados retornam `500` com uma mensagem genérica, e o detalhe técnico fica apenas no log.

### Coleção Postman

A coleção `docs/postman/user-management-api.postman_collection.json` cobre os principais cenários (autenticação, criação, permissões, endereços, listagem, validação de CEP, erros e exclusão lógica), com verificações automáticas. Por usar H2 em memória, reinicie a aplicação antes de rodar a coleção completa. Os cenários de CEP consultam o ViaCEP de verdade e exigem acesso à internet.

## Autor

Eric Lima dos Santos
