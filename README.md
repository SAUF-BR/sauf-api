# sauf-api

API do SAUF-BR (Java 21 + Spring Boot 4 + PostgreSQL: local em Docker ou Supabase). Decisões e pendências em
[docs/DECISOES.md](docs/DECISOES.md).

## Rodando localmente

1. Suba o banco local com Docker (seção abaixo). Para usar o Supabase compartilhado, copie a
   connection string do **Session pooler** (JDBC); o bloco já está comentado no `.env.example`.
2. Copie `.env.example` para `.env` e preencha o `SAUF_API_KEY` e o `SAUF_JWT_SECRET` (ver "Login").
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

### Usuário de homologação

Conta fixa com assinatura **Premium ativa até 31/12/2099** (D-83), para testar as telas de
assinante e o login: e-mail `homologacao@sauf.local`, senha `Homolog@2026` (só de homologação).
Rodar o script de novo volta a conta ao estado original.

```
docker compose exec -T postgres psql -U sauf -d sauf < docs/homologacao/usuario-homologacao.sql
```

Depois é só entrar pela tela de login (ou `POST /api/v1/auth/login`) com essas credenciais.

## Login

Login e cadastro próprios (D-84 a D-87): a sessão é um JWT num cookie `HttpOnly` de 7 dias.

- O `.env` precisa de `SAUF_JWT_SECRET` (32 bytes aleatórios em base64; sem ele a API não
  sobe). Para gerar: `openssl rand -base64 32` (ou, no PowerShell,
  `$b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)`).
  Cada ambiente usa o seu.
- Exigem login: `/me/**` e `/assinatura/**` (menos `/assinatura/planos`). Sem sessão: **401**.
- Senha: 8+ caracteres, com pelo menos 1 letra e 1 caractere especial.
- Em produção (HTTPS), use `SAUF_COOKIE_SECURE=true`.
- No Postman/curl, o cookie volta no `Set-Cookie` do login e vai nas próximas chamadas.
- Há "esqueci minha senha" (D-90) e troca de senha no perfil (D-91). O e-mail da conta não
  pode ser trocado (D-82). A verificação do e-mail no cadastro ficou para depois (D-89).

## Administração

As rotas `/api/v1/admin/**` (hoje só as do calendário, D-106) aceitam a `X-Api-Key` (é assim
que o scraper envia) ou a sessão de uma conta **ADMIN** (D-107). Para virar admin no seu
ambiente, ponha o seu e-mail no **seu** `.env` (nunca no repositório) e use uma rota de admin
logado com essa conta:

```
SAUF_ADMIN_EMAILS=seu-email@exemplo.com
```

Vários e-mails: separados por vírgula. Tirar o e-mail da lista não rebaixa a conta (troque o
`usuario.papel` no banco). Sem chave nem sessão: **401**; logado sem ser admin: **403**.

## E-mails ("esqueci minha senha")

O "esqueci minha senha" manda um **código de 6 dígitos** por e-mail; com o código certo a pessoa
entra e cria a senha nova no perfil (D-90).

**Local (padrão):** o `docker compose up -d` sobe também o **Mailpit**, um servidor de e-mail falso.
Tudo o que a API envia aparece em **http://localhost:8025** (nada sai para a internet).

**E-mail de verdade (Gmail):**

1. Numa conta Gmail do projeto, ative a verificação em duas etapas.
2. Gere uma "senha de app" em https://myaccount.google.com/apppasswords (16 letras).
3. Coloque no `.env` (a senha de app é segredo: nunca vai para o Git):

```
SAUF_MAIL_HOST=smtp.gmail.com
SAUF_MAIL_PORT=587
SAUF_MAIL_AUTH=true
SAUF_MAIL_STARTTLS=true
SAUF_MAIL_USER=conta.do.projeto@gmail.com
SAUF_MAIL_PASSWORD=<senha de app>
SAUF_MAIL_FROM=SAUF <conta.do.projeto@gmail.com>
```

4. Reinicie a API. Sem essas linhas, os e-mails continuam indo para o Mailpit.

## Termos de Uso e Política de Privacidade

Os textos ficam em `src/main/resources/documentos-legais/`, um arquivo `.md` por versão (D-93).
Ao subir, a API copia cada versão para o banco com o hash do texto, e os aceites guardam a versão
exata que cada pessoa aceitou.

**Para publicar uma versão nova:** copie o arquivo atual com o número novo (ex.:
`termos-de-uso-v1.1.0.md`), mude o texto e o cabeçalho e faça o deploy. **Não edite** uma versão
que já foi aceita: a API não sobe e avisa.

```
---
tipo: TERMOS_USO              # ou POLITICA_PRIVACIDADE
versao: 1.1.0
vigente_a_partir_de: "2026-12-01"   # pode ser no futuro: passa a valer nesse dia
exige_novo_aceite: true       # true = quem já tem conta aceita de novo ao entrar
resumo_alteracoes: "O que mudou, em uma frase (aparece para quem precisa aceitar de novo)"
---
# Termos de Uso do SAUF-BR
...
```

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
uv run sauf-scraper calendario   # datas do calendário (dados/calendario.toml)
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
├── usuario/      # conta do usuário logado: /me e foto de perfil (D-80 a D-82)
├── auth/         # login, cadastro, confirmação de e-mail, senha e segurança (D-84 a D-91)
├── email/        # envio de e-mails por SMTP e textos dos e-mails (D-88)
├── documento/    # Termos de Uso e Política de Privacidade versionados e aceites (D-93)
├── calendario/   # calendário de vestibulares e próximos prazos (D-104 a D-106)
├── admin/        # acesso às rotas /api/v1/admin/** (D-107)
└── ingestao/     # POST /api/v1/ingestao/censo e /lotes (usados pelo sauf-scraper)
```
