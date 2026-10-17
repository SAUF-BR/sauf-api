# sauf-api

API do SAUF-BR (Java 21 + Spring Boot 4 + PostgreSQL: local em Docker ou Supabase). Decisões e pendências em
[docs/DECISOES.md](docs/DECISOES.md).

## Rodando localmente

1. Suba o banco local com Docker (seção abaixo). Para usar o Supabase compartilhado, copie a
   connection string do **Session pooler** (JDBC); o bloco já está comentado no `.env.example`.
2. Copie `.env.example` para `.env` e preencha o `SAUF_API_KEY`.
3. Suba a API:

```
mvn spring-boot:run
```

Na primeira execução o Flyway cria as tabelas (`src/main/resources/db/migration`).

- Swagger: http://localhost:8080/api/v1/swagger
- Exemplo: http://localhost:8080/api/v1/areas

## Banco local com Docker

Cada pessoa usa o próprio Postgres 17 em Docker (`docker-compose.yml`, container
`sauf-postgres`, porta **5433**, banco/usuário/senha `sauf`, só para dev local). Assim, uma
migration de branch não quebra a API dos colegas.

```
docker compose up -d       # sobe (e espera ficar "healthy": docker ps)
docker compose stop        # para, mantendo os dados
docker compose down -v     # apaga o container E os dados (zera o banco)
```

- O `.env` aponta para `jdbc:postgresql://localhost:5433/sauf` (ver `.env.example`).
- O Flyway cria as tabelas sozinho ao subir a API (`mvn spring-boot:run`).
- O banco local começa **vazio**: popule na ordem API no ar → `censo` → spider → `igc` → `sisu`
  (ver "Carregando dados").
- **Regra do time:** o Supabase (compartilhado) recebe só o que já está na `main`.

## Migrations

Ficam em `src/main/resources/db/migration` e o Flyway aplica ao subir a API. **V1, V2 e V3
não mudam.** Daqui para frente, o nome usa data e hora (D-52), para duas branches não criarem
o mesmo número:

```
V<AAAA>_<MM>_<DD>_<HHMM>__descricao.sql     ex.: V2026_10_17_1015__nota_corte_sisu.sql
```

O Flyway lê o `_` como separador de versão (vira `2026.10.17.1015`). Nunca edite uma migration
que já rodou em algum banco: crie outra.

| Erro ao subir a API | Causa | O que fazer |
|---|---|---|
| `Found more than one migration with version ...` | Dois arquivos com a mesma versão (ex.: duas branches criaram o mesmo nome). | Renomeie a sua com um horário novo **antes** de ela rodar em qualquer banco compartilhado. No banco local, `docker compose down -v` e suba de novo. |
| `Detected resolved migration not applied to database` (migration fora de ordem) | Chegou da `main` uma migration com versão **menor** que a última aplicada no seu banco. | No banco local: `docker compose down -v` e suba de novo (as migrations rodam em ordem). Não ligue o `outOfOrder`. |
| `Validate failed: Migration checksum mismatch` | Alguém editou uma migration que já tinha rodado. | Desfaça a edição e crie uma migration nova com a mudança. |

**Regra do time:** cada um testa migrations no **Postgres local** (seção acima). O Supabase
recebe só o que já está na `main`.

## Carregando dados

Os dados chegam pelo `sauf-scraper`, com o mesmo `SAUF_API_KEY` no `.env` dos dois projetos
e `SAUF_API_BASE_URL=http://localhost:8080` no scraper:

```
uv run sauf-scraper censo        # 1º: instituições e ofertas do Censo (INEP)
uv run sauf-scraper --ies uem    # 2º: dados do site da UEM
uv run sauf-scraper igc --arquivo fontes/IGC_2023.xlsx   # 3º: nota MEC (IGC)
uv run sauf-scraper sisu --arquivo fontes/portal-sisu-sisu-2025-inscricoes-e-notas-de-corte.xlsx  # 4º: cortes
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
