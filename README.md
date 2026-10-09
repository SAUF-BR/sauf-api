# sauf-api

API do SAUF-BR (Java 21 + Spring Boot 4 + PostgreSQL/Supabase). Decisões e pendências em
[docs/DECISOES.md](docs/DECISOES.md).

## Rodando localmente

1. Crie um projeto no Supabase e copie a connection string do **Session pooler** (JDBC).
2. Copie `.env.example` para `.env` e preencha `SAUF_DB_URL`, `SAUF_DB_USER`, `SAUF_DB_PASSWORD`
   e `SAUF_API_KEY`.
3. Suba a API:

```
mvn spring-boot:run
```

Na primeira execução o Flyway cria as tabelas (`src/main/resources/db/migration`).

- Swagger: http://localhost:8080/api/v1/swagger
- Exemplo: http://localhost:8080/api/v1/areas

## Carregando dados

Os dados chegam pelo `sauf-scraper`, com o mesmo `SAUF_API_KEY` no `.env` dos dois projetos
e `SAUF_API_BASE_URL=http://localhost:8080` no scraper:

```
uv run sauf-scraper censo        # 1º: instituições e ofertas do Censo (INEP)
uv run sauf-scraper --ies uem    # 2º: dados do site da UEM
```

Pré-requisitos, como conferir e erros comuns: seção "Como popular a API" do README do `sauf-scraper`.

## Testes

```
mvn test
```

Os testes usam H2 em modo PostgreSQL e as mesmas migrações do Flyway; não precisam de banco externo.
O H2 existe **só nos testes** (`<scope>test</scope>` no `pom.xml`). Ao subir a API, ela exige
`SAUF_DB_URL` começando com `jdbc:postgresql://` e para com uma mensagem clara se não estiver
assim (`config/ExigirPostgres.java`).

## Estrutura

```
src/main/java/br/com/sauf_api/
├── comum/        # formato de erro, paginação, enums em minúsculas no JSON
├── config/       # CORS, conversão de parâmetros, Swagger, API key da ingestão
├── area/         # áreas do Censo (Cine/Unesco)
├── curso/        # curso genérico (catálogo)
├── instituicao/  # universidades (rotas /universidades)
├── oferta/       # curso ofertado por uma universidade + números agregados
└── ingestao/     # POST /api/v1/ingestao/censo e /lotes (usados pelo sauf-scraper)
```
