# User Management

Aplicação fullstack de gerenciamento de usuários e endereços, desenvolvida como teste técnico para a vaga de Analista de Sistemas Java Pleno.

Cada usuário possui um ou mais endereços, validados pela API pública do [ViaCEP](https://viacep.com.br). A aplicação inclui autenticação e autorização por perfil, paginação, auditoria, exclusão lógica e tratamento padronizado de erros.

## Status do projeto

- [x] Backend: estrutura, perfis de configuração, migration e entidades
- [ ] Backend: segurança (JWT), CRUD, integração com ViaCEP, tratamento de erros
- [ ] Backend: testes e cobertura
- [ ] Frontend Angular
- [ ] Docker e Docker Compose

> Este README é atualizado conforme o desenvolvimento avança.

## Stack

| Camada | Tecnologias |
|---|---|
| Backend | Java 21, Spring Boot 4.1.1, Spring Data JPA, Spring Security, Bean Validation, MapStruct, Lombok |
| Banco de dados | PostgreSQL (produção), H2 em modo PostgreSQL (desenvolvimento e testes), Flyway |
| Documentação da API | OpenAPI / Swagger UI (springdoc) |
| Frontend | Angular |
| Infraestrutura | Docker e Docker Compose |

## Estrutura do repositório

```
user-management/
├── backend/
│   └── user-management-api/   # API Spring Boot
├── frontend/                  # Aplicação Angular (em desenvolvimento)
├── docs/
│   └── decisions.md           # Decisões de arquitetura
└── README.md
```

## Como executar

### Pré-requisitos

- JDK 21
- Maven (ou o wrapper `mvnw` incluído no projeto)

### Backend em modo de desenvolvimento (H2)

```bash
cd backend/user-management-api
./mvnw spring-boot:run
```

No Windows (PowerShell):

```powershell
cd backend\user-management-api
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

## Principais decisões de arquitetura

As decisões completas, com o raciocínio de cada uma, estão em [docs/decisions.md](docs/decisions.md). Resumo até o momento:

- **Spring Boot 4.1.1 e Java 21**, por serem a linha atual do framework.
- **Flyway no controle do schema**, com `ddl-auto: validate`, para que o Hibernate apenas confira o mapeamento.
- **H2 em modo PostgreSQL** no desenvolvimento, para reduzir diferenças em relação à produção.
- **Exclusão lógica** com a coluna `deleted` e `@SQLRestriction`, que filtra os registros excluídos em todas as consultas. O `@SQLDelete` funciona como proteção caso alguém chame a exclusão física por engano.
- **Auditoria** com `AuditorAware` (`createdAt`, `updatedAt`, `createdBy`, `updatedBy`).
- **Tabela `users`**, porque `user` é palavra reservada no PostgreSQL.
- **E-mail único global**, mesmo para usuários excluídos logicamente, comportamento idêntico no H2 e no PostgreSQL.

## Autor

Eric Lima dos Santos
