# User Management API

API REST de gerenciamento de usuários e endereços, desenvolvida como parte de um teste técnico para a vaga de Analista de Sistemas Java Pleno.

Cada usuário possui um ou mais endereços, validados pela API pública do [ViaCEP](https://viacep.com.br). A API inclui autenticação e autorização por perfil, paginação, auditoria, exclusão lógica e tratamento padronizado de erros.

> O frontend (Angular) fica em um repositório separado: _link a adicionar_.

## Status do projeto

- [x] Estrutura do projeto, perfis de configuração, migration e entidades
- [x] Autenticação com JWT (login, emissão de token, rotas protegidas) e criação do ADMIN inicial
- [x] Autorização por perfil nos endpoints (ADMIN e USER)
- [x] CRUD de usuários e endereços (criar, buscar por id, atualizar e excluir)
- [ ] Listagem com paginação, filtros e ordenação
- [ ] Integração com o ViaCEP e cache
- [ ] Tratamento de erros (Problem Details) e documentação OpenAPI
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

- Swagger UI: http://localhost:8080/swagger-ui.html
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
| `GET` | `/users/{id}` | ADMIN ou o próprio usuário | Busca usuário com seus endereços |
| `PUT` | `/users/{id}` | ADMIN ou o próprio usuário | Atualiza usuário e endereços |
| `DELETE` | `/users/{id}` | ADMIN | Exclusão lógica do usuário e dos endereços (`204`) |

A listagem paginada com filtros e ordenação está em desenvolvimento.

### Regras de negócio

- **Perfis:** ADMIN tem acesso completo. USER visualiza e edita apenas os próprios dados e não pode alterar o próprio perfil (`role`) nem o `status`.
- **Endereços:** um usuário pode ter vários, mas apenas um é o principal. Se nenhum for marcado, o primeiro vira principal. Mais de um marcado é rejeitado.
- **Atualização de endereços:** com `id`, atualiza o existente. Sem `id`, cria um novo. Os endereços omitidos da lista são excluídos logicamente.
- **E-mail:** único e gravado em minúsculas. Permanece reservado mesmo após a exclusão lógica do usuário. Não pode ser alterado depois do cadastro.
- **Exclusão lógica:** nada é removido do banco. Registros excluídos deixam de aparecer nas consultas.
- **Auditoria:** `createdAt`, `updatedAt`, `createdBy` e `updatedBy` são preenchidos automaticamente, e o usuário registrado é o e-mail do token.
- **Senha:** armazenada com hash BCrypt e nunca devolvida nas respostas.

### Códigos de resposta

| Código | Quando |
|---|---|
| `200` / `201` / `204` | Sucesso (consulta e atualização / criação / exclusão) |
| `400` | Dados inválidos (validação dos campos) |
| `401` | Token ausente, inválido ou expirado, ou credenciais inválidas no login |
| `403` | Usuário autenticado sem permissão para o recurso ou a alteração |
| `404` | Usuário não encontrado (inclusive excluído) |
| `409` | E-mail já cadastrado |
| `422` | Regra de negócio violada (ex.: mais de um endereço principal) |

### Coleção Postman

A coleção `docs/postman/user-management-api.postman_collection.json` cobre os principais cenários (autenticação, criação, permissões, endereços e exclusão lógica), com verificações automáticas. Por usar H2 em memória, reinicie a aplicação antes de rodar a coleção completa.

## Autor

Eric Lima dos Santos
