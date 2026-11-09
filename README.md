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
uv run sauf-scraper censo        # 1º: instituições (com endereço da sede) e ofertas do Censo (INEP)
uv run sauf-scraper --ies uem    # 2º: dados do site da UEM
uv run sauf-scraper igc --arquivo fontes/IGC_2023.xlsx   # 3º: nota MEC (IGC)
uv run sauf-scraper sisu --arquivo fontes/portal-sisu-sisu-2025-inscricoes-e-notas-de-corte.xlsx  # 4º: cortes
```

Pré-requisitos, como conferir e erros comuns: seção "Como popular a API" do README do `sauf-scraper`.

## Pagamentos com Stripe (modo teste)

O fluxo usa Checkout hospedado, Customer Portal e webhooks (ADR-BE-016, D-69 a D-79). A assinatura
só nasce e muda de status quando o webhook chega. Sem `STRIPE_SECRET_KEY`, os pagamentos ficam
desabilitados e o resto da API funciona normalmente.

Use sempre chaves `sk_test_...` e o **Postgres local**: o Supabase só recebe o que já está na `main`.

### 1. Preparar o Stripe (uma vez por conta)

1. No Dashboard, em modo teste, crie o Product "Assinatura SAUF.BR" com três preços mensais: Basic,
   Plus e Premium.
2. Em **Settings > Billing > Customer portal**, salve a configuração do portal. Sem ela, o Stripe
   recusa a sessão e `POST /assinatura/portal` responde **502**. Regras da ADR:
   - troca entre os 3 preços liberada;
   - upgrade imediato, com cobrança proporcional;
   - downgrade agendado para o fim do ciclo;
   - cancelamento no fim do período.
3. Instale a [Stripe CLI](https://docs.stripe.com/stripe-cli) e rode `stripe login`.

### 2. Subir tudo

Em um terminal, encaminhe os webhooks para a API local:

```
stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe
```

O comando imprime um segredo `whsec_...`. Coloque-o em `STRIPE_WEBHOOK_SECRET` no `.env`, junto com
`STRIPE_SECRET_KEY` e os `STRIPE_PRICE_*` (ver `.env.example`). Em outro terminal, suba a API:

```
docker compose up -d
mvn spring-boot:run
```

Deixe o `stripe listen` aberto durante o teste. Se o segredo mudar, reinicie a API. Se o log da API
disser que os pagamentos estão desabilitados, falta a chave ou algum preço.

### 3. Testar a compra

```
curl -X POST http://localhost:8080/api/v1/assinatura/checkout \
  -H "Content-Type: application/json" \
  -d '{"plano":"basic","aceiteTermo":true}'
```

Abra a `url` devolvida e pague com o cartão de teste `4242 4242 4242 4242`, qualquer validade futura
e qualquer CVC. O `stripe listen` deve mostrar os eventos com **200**. Depois confira:

```
curl http://localhost:8080/api/v1/assinatura
docker exec sauf-postgres psql -U sauf -d sauf -c "select plano, status, renova_em, cancelar_no_fim_periodo from assinatura"
docker exec sauf-postgres psql -U sauf -d sauf -c "select stripe_invoice_id, valor_centavos, status from pagamento"
docker exec sauf-postgres psql -U sauf -d sauf -c "select id, tipo, processado_em from stripe_event order by recebido_em desc limit 10"
```

### 4. Testar o resto do ciclo

Abra o portal com `curl -X POST http://localhost:8080/api/v1/assinatura/portal` e use a `url` devolvida.

| Ação | Onde | Resultado esperado |
|---|---|---|
| Trocar de plano | Portal | `assinatura.plano` muda; no downgrade, só no fim do ciclo |
| Cancelar | Portal | `status = CANCELAMENTO_AGENDADO` e `cancelar_no_fim_periodo = true` |
| Encerrar na hora | `stripe subscriptions cancel sub_...` | `status = CANCELADA` |
| Reembolso integral | Dashboard, na cobrança | `pagamento.status = REEMBOLSADO`; a assinatura não muda |
| Reenviar um evento | `stripe events resend evt_...` | **200**, sem processar de novo |

Hoje todas as chamadas usam o mesmo usuário provisório (D-55). Para comprar de novo, encerre a
assinatura anterior; enquanto ela estiver aberta, o checkout responde **409**.

### Erros comuns

| Sintoma | Causa | O que fazer |
|---|---|---|
| Webhook responde **400** | `STRIPE_WEBHOOK_SECRET` diferente do impresso pelo `stripe listen` | Corrija o `.env` e reinicie a API |
| `/checkout` ou `/portal` responde **503** | Falta a chave ou o preço do plano | Confira o `.env` e reinicie a API |
| `/portal` responde **502** | Configuração do portal não salva em modo teste | Faça o item 2 da preparação |
| Pagou, mas o `GET /assinatura` responde **204** | O webhook não chegou | Confira se o `stripe listen` está aberto |
| `stripe trigger` responde **200**, mas nada muda | O evento de exemplo não traz `usuario_id` e é ignorado (D-76) | Teste pelo checkout de verdade |

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
├── assinatura/   # planos e assinatura do usuário
├── pagamento/    # Stripe: checkout, portal e webhooks
├── area/         # áreas do Censo (Cine/Unesco)
├── curso/        # curso genérico (catálogo)
├── instituicao/  # universidades (rotas /universidades)
├── oferta/       # curso ofertado por uma universidade + números agregados
└── ingestao/     # POST /api/v1/ingestao/censo e /lotes (usados pelo sauf-scraper)
```
