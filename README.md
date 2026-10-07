# User Management API

API REST de gerenciamento de usuários e endereços, desenvolvida como parte de um teste técnico para a vaga de Analista de Sistemas Java Pleno.

Cada usuário possui um ou mais endereços, validados pela API pública do [ViaCEP](https://viacep.com.br). A API inclui autenticação e autorização por perfil, paginação, auditoria, exclusão lógica e tratamento padronizado de erros.

> O frontend (Angular) fica em um repositório separado: _link a adicionar_.

## Status do projeto

- [x] Estrutura do projeto, perfis de configuração, migration e entidades
- [ ] Segurança (JWT e autorização por perfil)
- [ ] CRUD de usuários e endereços
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

| Variável | Descrição |
|---|---|
| `DB_URL` | URL JDBC do PostgreSQL |
| `DB_USER` | Usuário do banco |
| `DB_PASSWORD` | Senha do banco |

```bash
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

### Docker

_Será documentado após a conclusão do Docker Compose._

### Testes

_Será documentado após a implementação dos testes._

## Autor

Eric Lima dos Santos
