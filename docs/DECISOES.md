# Decisões do backend (API SAUF-BR)

Registro das decisões tomadas antes de reestruturar a API e do que ainda está em aberto.
Cada decisão tem um código (`D-xx`) para ser citada em cards, PRs e na documentação do TCC.

- Última atualização: 08/10/2026
- Responsável pelas decisões: Carlos
- Referências: protótipo da tela de cursos, DER atual (`diagrama-er.mermaid`), repositório
  `sauf-web-front` (mocks e ADRs) e `sauf-scraper` (`docs/MAPEAMENTO_CENSO.md`).

---

## 1. Contexto

O código atual deste repositório é de uma versão antiga do projeto (MySQL, anúncios, schema em
`db.md`) e **não serve mais de referência**. O modelo de negócio mudou: a plataforma passa a
focar em **assinaturas de estudantes** (área do assinante com simulados), e a v1 da API foca em
**apresentar dados de cursos e universidades** para o front.

Visão geral dos sistemas:

```
INEP (Censo) ──► sauf-scraper (Python) ──┐
Sites das IES ─► sauf-scraper (Python) ──┼──► API SAUF (Spring Boot) ──► Supabase (Postgres)
                                         │            ▲
                                         │            │ JSON (/api/v1)
                                         │      sauf-web-front (React)
```

---

## 2. Decisões tomadas

### Arquitetura e infraestrutura

| Código | Decisão | Motivo |
|---|---|---|
| D-01 | O **Supabase é usado só como banco Postgres**. Login, regras e acesso a dados ficam na API própria. | Mais controle e uma arquitetura mais clara para apresentar. |
| D-02 | O front (`sauf-web-front`) fala **só com a API**, nunca direto com o Supabase. | Um único lugar com as regras e um contrato de API documentável. |
| D-03 | O **Supabase ainda não foi criado**. A API fica preparada para ele: conexão por variáveis de ambiente, sem nada específico de Supabase no código. | O time nunca usou Supabase; a API funciona com qualquer Postgres. |
| D-04 | Segredos (senha do banco, chaves) ficam em **variáveis de ambiente / `.env` fora do Git**. | O projeto antigo tinha a senha do banco commitada. |

### Stack

| Código | Decisão | Motivo |
|---|---|---|
| D-05 | **Java 21** (LTS). | Suporte amplo em serviços de deploy e entre os colegas. |
| D-06 | **Spring Boot 4.x** (versão estável atual da linha 4). | A linha 3.x já saiu do suporte gratuito. |
| D-07 | **Maven**. | Já usado pelo time. |
| D-08 | Dependências base: Spring Web, Spring Data JPA, Bean Validation, driver PostgreSQL, **Flyway** e **springdoc-openapi** (Swagger). | Flyway versiona cada mudança do banco em `.sql`; o Swagger documenta as rotas para o front. |
| D-09 | DTOs de entrada e saída como **records** do Java. | Imutáveis e sem código repetitivo. |

### Repositório e processo

| Código | Decisão | Motivo |
|---|---|---|
| D-10 | Manter o repositório **`sauf-api`** (preserva o histórico), substituindo o código antigo. | Histórico do projeto. |
| D-11 | Por enquanto, trabalhar direto na **`main`**. Os cards do backend serão criados após discussão com o time (07/10/2026). | Agilidade até os cards existirem. |
| D-12 | Pouco comentário no código: só onde algo é específico e difícil de entender. | Preferência do time; o código e os testes devem se explicar. |

### API

| Código | Decisão | Motivo |
|---|---|---|
| D-13 | Todas as rotas com **prefixo de versão: `/api/v1/...`**. | Permite evoluir o contrato sem quebrar o front. |
| D-14 | A API expõe **`/universidades`** (nome usado pelo front e pelo produto); no banco a entidade continua `Instituicao`, como no DER. | O front já usa "universidades" nas telas e rotas. |
| D-15 | Contrato de comunicação conforme a **ADR-010 do front**: erro no formato `{ status, error, message, fields }`, paginação no padrão do Spring Data (`content`, `page`, `size`, `totalElements`, `totalPages`) e datas em **ISO 8601 UTC**. | Permite ao front tratar erros e paginação de forma centralizada. |
| D-16 | Documentação das rotas via **Swagger/OpenAPI** (springdoc). | Disponível para o front desde a primeira rota. |

### Modelo de dados

| Código | Decisão | Motivo |
|---|---|---|
| D-17 | **Catálogo em dois níveis**: `Curso` = curso genérico (ex.: Biomedicina, área Saúde) e `OfertaCurso` = o curso numa universidade (ex.: Biomedicina na UEM, com turno, duração, link). | Fiel ao protótipo: o card "Biomedicina" mostra "14 universidades ofertam na sua região". |
| D-18 | **Áreas oficiais do Censo** (classificação Cine/Unesco, `NO_CINE_AREA_GERAL`) como categorias dos cursos. | Fonte oficial e padronizada. Pode mudar se der problema. |
| D-19 | **UUID** como chave primária das tabelas. | Escala melhor e não expõe a quantidade de registros. O código e-MEC e a chave do scraper ficam como campos únicos à parte. |
| D-20 | **Duração em dois campos**: texto como aparece no site (`"5 anos"`, `"Bacharelado: 4 anos / Licenciatura: 5 anos"`) e em semestres (número) quando der para converter. | O texto mostra a informação real; o número permite o filtro de duração da tela. |
| D-21 | Campos do protótipo **sem fonte de dados ainda** (nota de corte SISU, mensalidade, nota MEC, imagens) existem no contrato, mas vêm **`null`** na v1. | O front já trata `null`; as fontes serão definidas depois. |

### Dados e ingestão (decididos no `sauf-scraper`)

| Código | Decisão |
|---|---|
| D-22 | Instituições vêm do **Censo da Educação Superior (INEP)**, só as que têm spider no scraper. |
| D-23 | O **site da instituição é a fonte principal** das ofertas. Oferta encontrada no site e ausente do Censo é **aceita** sem código e-MEC. |
| D-24 | Quando site e Censo discordam, **vale o site**. O Censo só completa o que o site não mostra (vagas, área, gratuidade). |
| D-25 | Identidade de uma oferta: **código e-MEC** (`CO_CURSO`, opcional) e **`chave`** gerada pelo scraper (ex.: `uem:engenharia-de-software:maringa:noturno`). Upsert por código e-MEC quando existe; senão por (instituição + chave). |
| D-26 | Cada oferta guarda o **link da página no site da instituição** (`urlOrigem`), para o botão "acessar o site". |
| D-27 | Lote de uma instituição não cadastrada é **recusado e registrado no log**. |

### Decididos na estruturação (06/10/2026)

| Código | Decisão | Motivo |
|---|---|---|
| D-28 | Banco de desenvolvimento: **um projeto Supabase de desenvolvimento** (resolve P-01). Os testes automatizados usam H2 em modo PostgreSQL, sem precisar de banco externo. | É o mesmo banco que vai para produção; Docker não está disponível. |
| D-29 | **Sem Lombok**: entidades com getters/setters explícitos e DTOs como records (resolve P-02). | Menos "mágica" para explicar e revisar. |
| D-30 | **Enums em minúsculas no JSON** (`presencial`, `ead`, `bacharelado`, `tecnologo`, `publica`), aceitos também nos filtros da URL (resolve P-03). No banco ficam em maiúsculas. | É o formato que o front já usa nos mocks. |
| D-31 | **Curso genérico do catálogo = "Cine + nome amigável"**: o rótulo Cine do Censo (`CO_CINE_ROTULO`) agrupa as ofertas e define a área; uma tabela pequena revisada à mão dá o nome exibido no card (ex.: rótulo "Contabilidade" para "Ciências Contábeis"). Resolve a parte de agrupamento de P-05. | Agrupamento oficial e sem duplicatas, com nomes amigáveis para o usuário. |
| D-32 | ~~"Uma página, vários": um curso por código e-MEC.~~ **Substituída por D-34** (08/10/2026). | — |
| D-33 | **O site da instituição é a fonte de verdade; o Censo é só complemento** (08/10/2026). Nas palavras do Carlos: *"Dê preferência ao que está no site da UEM, é o mais atualizado. Vamos usar o CSV como complemento apenas. 2024 para 2026 é muito tempo para confiarmos no senso."* Consequências: (1) a tabela de casamento só confirma códigos cujo grau/turno aparece na página do site; (2) graus e turnos vêm da página; os do Censo só entram quando a página não informa; (3) o Censo **não cria ofertas**: os cursos dele ficam na tabela `censo_curso` e só completam as ofertas do site. Fecha a pergunta "curso no Censo mas não no site aparece?": não. | O Censo mais recente é de 2024; o site é atualizado pela própria universidade. |
| D-34 | **Uma página do site = uma oferta**, com a lista de **opções** que a página mostra, cada uma com `grau` e `turno` (ex.: Educação Física na UEM: licenciatura "Integral ou Noturno" e bacharelado "Integral"). Substitui a D-32. Pedido do Carlos (08/10/2026): *"é totalmente desnecessário listar 4 cursos IGUAIS, queremos dar uma visão geral sobre o curso ofertado pela UEM"*. | O usuário vê o curso uma vez, com as opções e o link oficial para se informar. |
| D-35 | **Códigos e-MEC da página ficam numa lista interna** (`oferta_codigo_emec`), fora da API pública (08/10/2026, "Manter interno"). Servem só para ligar a oferta ao Censo: curso genérico e área (D-31), vagas (soma) e gratuidade. | Sem eles, toda oferta ficaria sem área e o filtro de área do catálogo pararia de funcionar. Para descartar: apagar a coleção `codigosEmec` e `completarComCenso`. |
| D-36 | **Bacharelado e "formação de professor" no mesmo curso genérico, com a área do bacharelado** (08/10/2026, opção "b"). Ex.: os rótulos Cine "Física" (Ciências naturais) e "Física formação de professor" (Educação) viram o curso "Física", área Ciências naturais. Quando só existe a licenciatura, o curso usa o nome sem o sufixo e a área dela. | O usuário procura "Física", não "Física formação de professor". |
| D-37 | **Texto "Sobre o Curso" da página do site** guardado na oferta (`oferta_curso.sobre`) e devolvido nas rotas de ofertas (08/10/2026, pedido do Carlos). **Provisório no formato:** texto puro, sem HTML; `\n` entre linhas e `\n\n` entre parágrafos; `null` quando a página não tem a seção. Só o site preenche (D-33); o Censo não mexe; o lote seguinte sobrescreve. "Mercado de Trabalho" fica em campo próprio (D-46). | Dá ao estudante a visão geral do curso, com o link oficial ao lado indicando a fonte. Texto puro evita injeção de HTML no front. |

### Decididos pela análise do protótipo (09/10/2026)

Vêm da comparação entre as telas do protótipo e os dados que temos (`05-prototipo-x-dados.md`).

| Código | Decisão | Motivo |
|---|---|---|
| D-38 | **Favorito aponta para a oferta e para a universidade** (resolve P-27). O estudante salva o curso numa universidade (ex.: "Biomedicina · UEM"), não o curso genérico, e salva universidades à parte. | É o que as telas "Meus favoritos" (abas Cursos e Universidades) mostram. |
| D-39 | **Visão geral do curso genérico usa o texto da UEM** (P-30, opção a), com o rótulo "Visão geral baseada na UEM" e o link da página de origem. | Não existe texto genérico oficial; o da universidade é real e tem fonte. |
| D-40 | **"Mercado e salários" fica fora do MVP** (P-31, opção a): a aba fica escondida no front. | Depende de RAIS/CAGED cruzado com CBO, um projeto à parte. |
| D-41 | **"Você vai ter aulas de" e "Onde você pode trabalhar" mostram o texto do site**, não tags (P-32, opção a). | Evita curadoria manual de tags; o site já traz o texto. |
| D-42 | **"Na sua região" = mesmo estado do usuário** (P-34, opção a). O usuário cadastra cidade e UF no perfil. Resolve P-13. | Simples e sem pedir localização ao navegador. |
| D-43 | **O Censo continua só para IES com spider** (P-35, opção c). A meta da apresentação é cerca de **5 IES completas**, em vez de muitas IES com poucos dados. | Qualidade da demonstração acima de quantidade. |
| D-44 | **Cursos parecidos pela hierarquia Cine + nota de corte, sem "%"** (P-36, opção a). | Critério explicável; o "% parecido" do protótipo não tem base nos dados. |
| D-45 | **Nota de corte do SISU só de ampla concorrência** (P-37, opção a). | Um número por oferta e ano, comparável entre universidades. |
| D-46 | **Texto "Mercado de Trabalho" em campo próprio** (`oferta_curso.mercado_trabalho`, `mercadoTrabalho` na API), separado do `sobre` (09/10/2026, opção A). Mesmo formato e regras do `sobre` (D-37): texto puro, só o site preenche, o Censo não mexe, o lote seguinte sobrescreve; `null` quando a página não tem a seção, e o front esconde o bloco. Alimenta "Onde você pode trabalhar" (D-41). Coluna criada na migração `V2__mercado_trabalho.sql`. | O front lê cada bloco direto, sem recortar texto nem depender de marcador. |
| D-47 | **"Campo de atuação" também conta como "Mercado de Trabalho"** (09/10/2026, decisão do Carlos). O scraper reconhece os dois títulos em negrito ou como linha exata de texto (com ou sem ":", maiúsculas ou minúsculas, com ou sem acento); frases comuns como "O campo de atuação do biomédico é…" não são cortadas. Se a página tiver as duas seções, elas vão juntas no `mercadoTrabalho`, separadas por `

### Scrapers gerais e notas (16/10/2026)

| Código | Decisão | Motivo |
|---|---|---|
| D-48 | **Importadores gerais leem um arquivo local baixado à mão** (`sauf-scraper <fonte> --arquivo ...`). São filtrados para as IES do escopo (as que têm spider, P-35 c / D-43) e casados pelo **código e-MEC**, com relatório no log das IES do escopo que não casaram. `--dry-run` grava `output/<fonte>-<ano>.json`; sem ele, envia para a API. | O gov.br bloqueia robôs; a planilha oficial baixada à mão é estável e auditável. |
| D-49 | **"Nota MEC" = IGC faixa (1 a 5) da IES**, guardado por ano na tabela `indicador_ies` (com o IGC contínuo), sem apagar anos anteriores. A API mostra em `notaMec` a faixa do **ano mais recente** importado (copiada para `instituicao.nota_mec` na ingestão). `'SC'` (sem conceito) vira `null`. Rota: `POST /api/v1/ingestao/igc`. | O IGC é o indicador oficial da instituição; guardar por ano permite histórico. |
| D-50 | **Notas de corte (fase B):** fonte Sisu (gov.br/mec/pt-br/sisu/documentos → "Inscrições e notas de corte"; a edição mais recente publicada é a Sisu 2025). Só **ampla concorrência** (`TP_MOD_CONCORRENCIA` 'A' / `TIPO_CONCORRENCIA` 'AC', D-45). `NU_NOTACORTE` 0 vira `null`. A nota é guardada por (código e-MEC, turno, ano). O card de cada oferta mostra o **menor** corte, com o turno. A "Média do corte SISU <ano>" do curso genérico é a **média simples** de todas as ofertas com nota, calculada no **backend**. Ofertas sem Sisu mostram "Ingresso por vestibular próprio". O rótulo usa o ano do dado, nunca um ano fixo. | Uma regra só para todas as telas, sem cálculo no front. |
| D-51 | **G4 (PROUni/FIES) e G5 (e-MEC) saem do plano.** As 5 IES são públicas. O endereço vem do arquivo de IES do Censo (`DS_ENDERECO_IES` etc., fase C). Ano de fundação, formas de ingresso, logo, foto e "sobre" da IES vão para `dados/ies/<sigla>.yaml` (fase D). "Sua chance estimada" fica fora do MVP; a distância "a X km" sai do protótipo; disciplinas = só o link do `urlOrigem`. | Corta fontes que não agregam às 5 IES públicas da apresentação. |
| D-52 | **Migrations com data e hora no nome** (17/10/2026): `V<AAAA>_<MM>_<DD>_<HHMM>__descricao.sql` (o Flyway lê `_` como separador: vira a versão `AAAA.MM.DD.HHMM`). V1, V2 e V3 ficam como estão. Não usamos `outOfOrder`. Cada um testa no Postgres local (Docker); o Supabase recebe só o que está na `main`. Erros comuns e o que fazer: README, seção "Migrations". | Branches paralelas criavam o mesmo número (V4, V4...). |
| D-53 | **Campos de nota de corte na API** (17/10/2026, complementa a D-50). Oferta: `notaCorteSisu` (menor corte entre os códigos e-MEC e turnos da oferta) + `notaCorteSisuTurno` + `notaCorteSisuAno`. Curso genérico: `notaCorteSisuMin` foi **trocado** por `notaCorteSisuMedia` (média simples, cada par (código e-MEC, turno) uma vez, arredondada em 2 casas) + `notaCorteSisuAno`. Sempre o ano mais recente que tem nota; `null` = sem Sisu (o front mostra "Ingresso por vestibular próprio"). As notas ficam na tabela `nota_corte_sisu` (rota `POST /api/v1/ingestao/sisu`, que devolve também `semOferta`); a coluna antiga `oferta_curso.nota_corte_sisu` ficou sem uso e pode sair numa migration futura. | Regras da D-50 aplicadas no backend. |

`, na ordem da página. "Perfil do egresso" e "Atribuições" continuam no `sobre`. | Várias páginas da UEM usam "Campo de atuação" no lugar de "Mercado de Trabalho" para o mesmo conteúdo. |

### Assinaturas (21/10/2026)

| Código | Decisão | Motivo |
|---|---|---|
| D-54 | **Planos de assinatura definidos no código** (`enum Plano` em `assinatura/`), não no banco. Preço, texto, selo e recursos são alterados no código e publicados com a API. Não se renomeia nem apaga uma constante que já tenha assinaturas (o banco guarda o nome dela). | Alterar os planos com facilidade, numa fonte só, validada pelos testes. |
| D-55 | **Login próprio na API** (resolve P-09): cadastro, login, logout e `GET /me` na própria API. O time define os dados do usuário e o que a API devolve. | Mais controle; coerente com a D-01 (Supabase só como banco). |
| D-56 | **Usuário sem assinatura: `GET /api/v1/assinatura` responde `204 No Content`.** | Diferencia "não tem assinatura" de erro, sem inventar um corpo vazio. |
| D-57 | **A assinatura renova (+1 mês) só com pagamento confirmado.** Sem confirmação, ela vence no `renovaEm` (o acesso vai até esse dia, inclusive) e passa a contar como sem assinatura. | Ninguém fica com acesso sem ter pago. |

---

## 3. Estrutura da v1 (implementada)

Primeira versão do código, em `src/main`. Pode mudar com as próximas decisões.

### Áreas do Censo 2024 (`CO_CINE_AREA_GERAL` / `NO_CINE_AREA_GERAL`)

| Código | Área |
|---|---|
| 00 | Programas básicos |
| 01 | Educação |
| 02 | Artes e humanidades |
| 03 | Ciências sociais, comunicação e informação |
| 04 | Negócios, administração e direito |
| 05 | Ciências naturais, matemática e estatística |
| 06 | Computação e Tecnologias da Informação e Comunicação (TIC) |
| 07 | Engenharia, produção e construção |
| 08 | Agricultura, silvicultura, pesca e veterinária |
| 09 | Saúde e bem-estar |
| 10 | Serviços |

### Tabelas da v1

- **`instituicao`**: `id` (UUID), `codigo_emec` (único), `nome`, `sigla`, `tipo` (pública/privada),
  `categoria_administrativa`, `organizacao_academica`, `uf`, `cidade`, `site_url`, `logo_url`,
  `nota_mec`, `ativa`, `atualizada_em`.
- **`area`**: `id`, `codigo_cine`, `nome` (áreas gerais do Censo).
- **`curso`** (genérico): `id`, `nome`, `area_id` (nulo para curso pendente de revisão),
  `codigo_cine_rotulo`, `pendente_revisao`, `imagem_url`.
- **`oferta_curso`** (uma página do site, D-34): `id`, `curso_id`, `instituicao_id`, `chave`
  (única por instituição), `nome_no_site`, `modalidade`, `turno` (texto da página),
  `duracao_texto`, `duracao_semestres`, `cidade`, `uf`, `vagas`, `gratuito`, `url_origem`,
  `nota_corte_sisu`, `mensalidade`, `sobre` (texto, D-37), `ativa`, `coletada_em`.
  - **`oferta_opcao`**: (`oferta_id`, `grau`) + `turno`. As opções que a página mostra.
  - **`oferta_codigo_emec`**: (`oferta_id`, `codigo_emec`). Uso interno (D-35).
- **`censo_curso`** (complemento, D-33): `codigo_emec` (PK), `instituicao_id`, `curso_id`, `nome`,
  `grau`, `modalidade`, `cidade`, `uf`, `vagas`, `gratuito`, `ano_censo`.

### Rotas da v1

| Rota | Uso no front |
|---|---|
| `GET /api/v1/cursos` | Listagem do catálogo (tela de cursos), paginada, com filtros `area`, `modalidade`, `grau`, `duracaoSemestres`, `uf`, `cidade`, `busca`. Cada item traz `totalUniversidades`, `modalidades`, `graus`, duração mínima/máxima em semestres e a menor nota de corte, calculados só com as ofertas que passam nos filtros. Sem filtro de nota de corte por enquanto (ver seção 4). |
| `GET /api/v1/cursos/{id}` | Detalhe do curso genérico (aceita `uf`/`cidade` para os números da região). |
| `GET /api/v1/cursos/{id}/ofertas` | Universidades que ofertam o curso, uma por página do site, com `opcoes` (`grau` + `turno`), o texto `sobre` (D-37) e o link "acessar o site". Filtros `modalidade`, `grau`, `uf`, `cidade`. |
| `GET /api/v1/universidades` | Listagem de universidades, paginada, com filtros `uf`, `cidade`, `tipo`, `modalidade`, `busca`. |
| `GET /api/v1/universidades/{id}` | Detalhe da universidade, com cursos por área. |
| `GET /api/v1/universidades/{id}/ofertas` | Cursos ofertados pela universidade (filtros `area`, `modalidade`, `grau`). |
| `GET /api/v1/areas` | Áreas para os filtros, com contagem de cursos. |
| `POST /api/v1/ingestao/censo` | Import do Censo (`sauf-scraper censo`). Cria/atualiza instituições pelo código e-MEC, cursos genéricos pelo rótulo Cine (D-31, D-36) e a tabela `censo_curso`. **Não cria ofertas** (D-33); depois do import, as ofertas do site já existentes são completadas de novo. Exige `X-Api-Key`. |
| `POST /api/v1/ingestao/lotes` | Lote do scraper. Cria/atualiza uma oferta por página pela `chave` (D-34), com `opcoes` e `codigosEmec`, e completa com o Censo (D-35). Instituição não cadastrada: **422**. Exige `X-Api-Key`. |
| `GET /api/v1/assinatura/planos` | Planos da tela de assinatura, do mais barato ao mais caro (D-54). |
| `GET /api/v1/assinatura` | Assinatura vigente do usuário: `{plano, status, renovaEm}`, ou **204** se não houver (D-56). |
| `PUT /api/v1/assinatura` | Assina ou troca de plano. Corpo `{ "plano": "plus" }`. Plano inexistente: **400**; o mesmo plano já ativo: **409**. |
| `POST /api/v1/assinatura/cancelar` | Agenda o cancelamento; o acesso continua até o `renovaEm`. Sem assinatura ou já agendado: **409**. |
| `POST /api/v1/assinatura/reativar` | Desfaz o cancelamento agendado. Se não estiver agendado: **409**. |

Ingestão: o segredo vem de `SAUF_API_KEY` (mesmo valor nos dois projetos). Sem ele, as rotas
de ingestão respondem 401. Resposta: `{criados, atualizados, inativados, rejeitados[]}`.

Completar com o Censo (D-35): a oferta procura em `censo_curso` os seus códigos e-MEC. O curso
genérico é o mais frequente entre eles; `vagas` é a soma; `gratuito` é verdadeiro se algum for.
Sem código ou sem Censo, a oferta fica num curso pendente de revisão (P-05).

Migrações: uma só, `V1__estrutura_inicial.sql`, com todas as tabelas acima e as 11 áreas do
Censo (09/10/2026, pedido do Carlos). Como o banco ainda não tinha sido criado, as migrações
de rascunho (V1 a V5) foram juntadas. A partir de agora, com o Supabase em uso, **toda mudança
no banco vira uma migração nova** (`V2__...`), nunca uma edição da V1.
Assinaturas: `V2026_10_21_2033__assinaturas.sql` (nome com data, D-52).

Paginação: `page` (começa em 0), `size` (padrão 12, máximo 50) e `sort` (ex.: `sort=nome,asc`).
Swagger: `/api/v1/swagger`.

---

## 4. Comportamentos provisórios (aguardando o Carlos)

Implementados para o trabalho seguir, mas **não são decisões**: cada um tem uma pendência
aberta e pode mudar sem esforço grande.

| Pendência | O que o código faz hoje (08/10/2026) | Onde |
|---|---|---|
| P-05 | Oferta do site **fora do Censo** entra no catálogo ligada a um curso genérico criado com o **nome do site**, **sem área** e marcado `pendente_revisao = true`. Ofertas com o mesmo nome reaproveitam esse curso. | `IngestaoService.cursoForaDoCenso` |
| D-31 (nome amigável) | O curso genérico criado pelo Censo usa o **nome do rótulo Cine** sem o sufixo "formação de professor" (D-36), ex.: "Engenharia civil", "Física". A ingestão nunca sobrescreve o nome depois de criado, então a revisão manual pode editar `curso.nome` direto. A tabela revisada ainda não existe. | `IngestaoService.cursoGenerico` |
| P-20 | Card do catálogo devolve duração **mínima e máxima** em semestres e a **menor** nota de corte. | `EstatisticasOfertas.porCurso` |
| P-25 | Duração convertida em semestres **no scraper** só quando o texto é simples ("5 anos" → 10, "3,5 anos" → 7); o texto original vai sempre em `duracaoTexto`. | `sauf-scraper`: `semestres_de_texto` |
| P-11 | **Provisório** (09/10/2026): filtro **`notaCorteMax`** de `GET /api/v1/cursos` **removido** enquanto não houver fonte de nota de corte; volta quando houver. Se o front mandar o parâmetro, ele é ignorado e a lista vem sem esse filtro. Os campos de resposta sem fonte (`notaCorteSisu`, `notaCorteSisuMin`, `mensalidade`, `notaMec`, `imagemUrl`, `logoUrl`) continuam no contrato, vindo `null` (D-21), e estão descritos no Swagger com `@Schema` ("Ainda sem fonte de dados; vem null (P-11)"). | `CursoController.listar`, `FiltroOfertas`, DTOs de resposta |
| P-26 | Oferta (página) ausente no lote seguinte fica **inativa** (não é apagada). | `IngestaoService.importarLote` |
| P-14 | Sem gateway de pagamento, `PUT /api/v1/assinatura` age como pagamento confirmado: ativa a assinatura por 1 mês (ou troca o plano mantendo a data). Volta a depender do gateway quando ele existir (D-57). | `AssinaturaService.trocarPlano` |
| D-55 | Até o login existir, todas as requisições usam um **usuário provisório** fixo e `assinatura.usuario_id` fica sem chave estrangeira. Com o login, muda só a classe `UsuarioLogado` e uma migração nova cria a chave estrangeira. | `comum/UsuarioLogado.java` |

## 5. Pontos pendentes

### Precisam de decisão antes ou durante a estruturação

| Código | Pendência | Observação / sugestão |
|---|---|---|
| ~~P-01~~ | Banco para desenvolvimento. | Resolvido: D-28. |
| ~~P-02~~ | Lombok. | Resolvido: D-29. |
| ~~P-03~~ | Formato dos enums no JSON. | Resolvido: D-30. O scraper também passou a mandar minúsculas (06/10/2026). |
| P-04 | **Grau `BACHARELADO_E_LICENCIATURA`**: existe no Censo, mas não no tipo do front. | Incluir no front ou tratar na API. |
| P-05 | **Ligação oferta → curso genérico** para ofertas **fora do Censo** (sem código e-MEC, sem rótulo Cine; ex.: Engenharia de Software da UEM). | Agrupamento das ofertas do Censo resolvido em D-31. Falta decidir: criar curso genérico novo? Com qual área? |
| P-06 | **Autenticação da ingestão** (rotas usadas pelo scraper). | Implementado com header `X-Api-Key` (segredo em `SAUF_API_KEY`), como sugerido. Falta o Carlos confirmar. |
| P-07 | **Turno**: texto livre (`"Matutino ou Noturno"`) ou lista de valores (matutino, vespertino, noturno, integral)? | A lista facilita filtro por turno. |
| P-08 | **Atualização do DER e do diagrama de classes** com o modelo em dois níveis (`Area`, `Curso`, `OfertaCurso`), UUID e os campos novos (código e-MEC, chave, link, grau, turno, duração em dois campos). | Os dois diagramas ainda têm `Instituicao → Curso` direto e ids `Long`. |
| P-20 | **Card do catálogo**: a tela mostra uma duração e uma nota só. A API devolve duração **mínima e máxima** em semestres e a **menor** nota de corte entre as ofertas. Serve, ou outro critério? | Perguntado em 06/10/2026. |
| P-21 | **Próximo passo do backend**: rotas de ingestão (`POST /api/v1/ingestao/censo` e `/lotes`) antes de outras rotas? | Perguntado em 06/10/2026. Carlos priorizou o scraper antes (06/10/2026). |
| P-22 | **Nome do pacote Java**: manter `br.com.sauf_api` ou trocar (ex.: `br.com.saufbr.api`)? | Perguntado em 06/10/2026. |
| P-23 | **Remoção do código antigo** (models, controllers, repositories, `db.md`, `application.properties`): confirmar antes do commit. | Remoção feita na cópia local, ainda sem commit. |
| P-24 | **Código Jira e branch** do commit do backend. | O scraper usa `TR-236`. |
| P-25 | **Duração em semestres**: converter no scraper ("5 anos" → 10)? | Perguntado em 06/10/2026. |
| P-26 | **Oferta que some do site** na coleta seguinte: marcar como inativa? | Sugestão: sim (some do catálogo, fica no banco). |
| ~~P-27~~ | **Favorito**: o estudante favorita o **curso genérico** ou a **oferta** (curso numa universidade)? | Resolvido: D-38. O DER ainda tem `CursoFavorito` ligado a `Curso` (atualizar junto com P-08). |
| P-28 | **`SequenciaEstudante`**: sequência de **acertos** em simulados ou de **dias** estudando (card "constância" do front)? | Vem do diagrama de classes. |
| P-29 | **Inconsistências do diagrama de classes**: multiplicidade de `Assinatura` (o DER está certo: 1 estudante → N assinaturas), herança `Usuario` (só no diagrama), `RespostaVocacional.opcaoEscolhida` (falta no DER), regras nas entidades × nos services. | Ver P-08. |

### Ficam para depois da v1

| Código | Pendência | Observação |
|---|---|---|
| ~~P-09~~ | **Login e autenticação**: Supabase Auth ou login próprio (Spring Security)? | Resolvido: D-55 (login próprio). Implementação pendente. |
| P-10 | **Contradição no front**: a ADR-005 decidiu token em cookie HttpOnly, mas o `client.ts` lê o token do `localStorage`. | Resolver junto com P-09. |
| P-11 | **Fontes de dados ainda inexistentes**: mensalidade, imagens dos cursos, logos das universidades. Nota MEC resolvida pela D-49 e notas de corte pela D-50/D-53. | Na v1 vêm `null` (D-21). |
| P-12 | **Formas de ingresso e próximos prazos/editais** por universidade. | `FormaIngresso` não tem relação no DER. |
| ~~P-13~~ | **"Na sua região"**: localização do usuário ou filtro de UF/cidade? | Resolvido: D-42 (UF do perfil). |
| P-14 | **Assinaturas e simulados**: planos, gateway de pagamento, conteúdo dos simulados. | Ainda em definição pelo time. |
| P-15 | **Deploy** da API e **agendamento** do scraper. | Onde rodam e com que frequência. |
| P-16 | **Ambientes**: um projeto Supabase ou dev e produção separados. | — |
| P-17 | **Histórico** de alterações de ofertas. | Sugestão para a v1: só o valor atual e a data da última coleta. |
| P-18 | **Branches e PRs**: quando sair da `main`, qual fluxo e padrão de commit (`TR-xxx`)? | Depende dos cards (D-11). |
| P-19 | **ADR-010 está como "proposto"** no front. | Validar formalmente com o time do front, agora que o backend adota o formato (D-15). |
| P-33 | **Calendário acadêmico** (eventos de inscrição, prova, resultado, feiras; datas nacionais × vestibulares). | Adiada em 09/10/2026: o formato do calendário ainda não foi definido. |

---

## 6. Alinhamentos necessários com o front

Itens que mudam algo no `sauf-web-front` quando a API real substituir os mocks:

1. `VITE_API_URL` passa a terminar em `/api/v1` (D-13).
2. A tela de cursos consome o catálogo em dois níveis (D-17): o tipo `Curso` do front hoje
   mistura curso genérico e oferta (`Curso.universidade`).
3. `grau` deixa de ser um valor só (D-34): o card do catálogo recebe `graus` (lista) e cada
   oferta recebe `opcoes: [{grau, turno}]`. Os códigos e-MEC não aparecem na API (D-35).
   A oferta ganha também o campo `sobre` (texto puro com `\n`/`\n\n`, ou `null`), para a tela de detalhe (D-37), e o campo `mercadoTrabalho` no mesmo formato, para o bloco "Onde você pode trabalhar" (D-46).
4. Ids passam a ser UUID (D-19). O front já trata ids como `string`.
5. Formato dos enums (P-03). O grau `bacharelado_e_licenciatura` deixa de ser usado pelas
   ofertas: com as opções em lista (D-34), bacharelado e licenciatura vêm como duas opções.
6. Áreas passam a ser as do Censo (D-18), por exemplo "Saúde e bem-estar" em vez de "Saúde".
