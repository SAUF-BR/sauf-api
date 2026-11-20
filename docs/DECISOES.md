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
| D-51 | **G4 (PROUni/FIES) e G5 (e-MEC) saem do plano.** As 5 IES são públicas. O endereço vem do arquivo de IES do Censo (`DS_ENDERECO_IES` etc., fase C). Ano de fundação, formas de ingresso, logo, foto e "sobre" da IES vão para `dados/ies/<sigla>.yaml` (fase D). **Feito na D-98 a D-103 (TR-343), em TOML** (formas de ingresso ainda não). "Sua chance estimada" fica fora do MVP; a distância "a X km" sai do protótipo; disciplinas = só o link do `urlOrigem`. | Corta fontes que não agregam às 5 IES públicas da apresentação. |
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

### Endereço da sede (26/10/2026)

| Código | Decisão | Motivo |
|---|---|---|
| D-58 | **Endereço da sede da IES vem do Censo** (fase C, 26/10/2026). O `censo` lê `DS_ENDERECO_IES`, `DS_NUMERO_ENDERECO_IES`, `DS_COMPLEMENTO_ENDERECO_IES`, `NO_BAIRRO_IES` e `NU_CEP_IES` do arquivo de IES (cidade e UF já vinham de `NO_MUNICIPIO_IES` e `SG_UF_IES`); vazio vira `null`. Fica na `instituicao` (`endereco_*`, migration `V2026_10_26_1925__endereco_instituicao.sql`) e sai só no detalhe da universidade, como objeto `enderecoSede` (logradouro, numero, complemento, bairro, cep, cidade, uf); a listagem continua com cidade e UF. CEP guardado e devolvido **só com os 8 dígitos**; a máscara (87020-900) é do front (decidido pelo Carlos em 26/10/2026). É o endereço da **sede**, não de cada campus, e por isso o campo se chama `enderecoSede`. O endereço completo aparece **só na página da universidade, com o rótulo "Sede"**. **Curso ou oferta de campus fora da sede (ex.: Umuarama, Goioerê) mostra só a cidade/UF do campus, nunca o endereço da sede, nem com o rótulo "Sede"**, para não indicar o lugar errado (opção recomendada, confirmada pelo Carlos em 26/10/2026). Hoje nenhuma resposta de oferta ou de curso expõe endereço; se um dia expuser, a oferta só leva o endereço da sede quando a cidade/UF dela for igual à da sede. | Fonte oficial que já usamos; dispensa o G5 (e-MEC). |

### Listagem de universidades (31/10/2026)

| Código | Decisão | Motivo |
|---|---|---|
| D-59 | **Filtro `notaMec` por faixa** em `GET /universidades`: `notaMec=4-5` (nota 4 ou 5) ou `notaMec=3`. Usa a `nota_mec` da instituição (IGC faixa, D-49). Outro valor responde **400** com o campo em `fields` (`enum FaixaNotaMec`). Sem o parâmetro, não filtra ("Todas"). | São as opções do filtro na tela de universidades. |
| D-60 | **Busca e cidade sem diferenciar maiúscula, acento e hífen.** O hífen vira espaço e a ordem das palavras importa ("pos-graduacao" acha "Pós-Graduação"). A regra fica num lugar só, no `FiltroUniversidades`: `normalizar()` trata o texto digitado (Java) e `normalizado()` trata a coluna (`translate` no banco, que funciona no Postgres e no H2). A busca procura no nome e na sigla. **Busca que fica vazia depois de normalizar (só "-" ou só espaços) é ignorada** e traz todas, igual à busca vazia. | Pedido no PR da TR-333; o usuário não precisa acertar acento nem hífen. |
| D-61 | **`tipo` e `modalidade` aceitam vários valores** (`tipo=publica,privada` ou o parâmetro repetido). Dentro do mesmo filtro vale "ou"; entre filtros diferentes, "e". Valor inválido: **400**. | Os filtros da tela são caixas de seleção múltipla. |
| D-62 | **`GET /universidades/contagens`** devolve `{tipo: {publica, privada}, modalidade: {presencial, semipresencial, ead}}` com os mesmos filtros da listagem, em **busca facetada**: a contagem de cada tipo ignora o filtro de tipo e a de cada modalidade ignora o filtro de modalidade; os outros filtros valem. | Mostrar quantas universidades cada opção traria. Se combinasse com o próprio grupo, as outras opções dariam sempre 0. |
| D-63 | **`GET /universidades/cidades?uf=PR`** devolve as cidades com universidade ativa no estado, sem repetir e em ordem alfabética. Ignora os outros filtros. Sem `uf`, responde `[]` com **200**. | Preencher o select de cidade depois de escolher o estado, sem o front precisar tratar erro antes disso. |

### Visão geral da universidade (31/10/2026)

| Código | Decisão | Motivo |
|---|---|---|
| D-64 | **`esfera`** no detalhe da universidade: `federal`, `estadual` ou `municipal`, tirada da `categoriaAdministrativa` do Censo; `null` para privadas. Aceita `publica_estadual` (scraper) e `Pública Estadual` (exemplo do Postman), com a mesma normalização da D-60. | O selo "Pública estadual" da tela, sem o front interpretar a categoria. |
| D-65 | **`vagasPorAno`** = soma das `vagas` das ofertas **ativas** que têm essa informação (Censo, D-35); `null` quando nenhuma tem. **`gratuita`** = `true` quando todas as ofertas ativas com essa informação são gratuitas (e pelo menos uma tem); senão `false`. | Números da universidade calculados no backend, com a mesma fonte das ofertas. |
| D-66 | **`modalidades` do detalhe é uma lista de objetos** `{modalidade, disponivel, totalCursos, descricao}`, sempre com as 3, na ordem presencial, semipresencial e ead. `descricao` vem `null` (sem fonte). A listagem continua com a lista de textos. | O bloco "Modalidades ofertadas" mostra as 3, marcando as indisponíveis. |
| D-67 | **Campos da visão geral sem fonte** ficam no contrato: `capaUrl`, `anoFundacao`, `notaFormasIngresso` e `proximoPrazo` vêm `null`; `sobre` e `formasIngresso` vêm `[]`. Fonte prevista: arquivo por IES da fase D (D-51) e P-12. A distância até o usuário **não entra** (D-51). | O front já desenha a tela completa e esconde o que vier vazio. |

### Banco de questões (08/11/2026)

| Código | Decisão | Motivo |
|---|---|---|
| D-68 | **Ingestão de questões** em `POST /api/v1/ingestao/questoes` (mesma `X-Api-Key`). Um lote por prova, identificada por `banca` + `ano` + `edicao` + `caderno` (ENEM: um lote por dia, `D1`/`D2`, caderno `LARANJA`; ADR-SC-010). A questão é identificada por `numero` + `lingua` (as questões 1 a 5 do ENEM existem em inglês e em espanhol). Reenviar **atualiza** a questão e **troca todas as alternativas**; questão que não veio no lote **fica** (não é apagada). O texto-base é guardado uma vez por prova (hash SHA-256 do conteúdo) e é apagado quando nenhuma questão o usa mais. Validações, que **recusam só a questão** (vai para `rejeitados` como `questão N: motivo`, o resto do lote segue): rótulo repetido; múltipla escolha com **exatamente 1 correta** (0 se anulada); somatória com `valor` em todas as alternativas e `gabaritoSoma` igual à soma das corretas; número repetido no lote. Enum inválido ou campo obrigatório faltando: **400** no lote todo. **`status`**: `disponivel` (pode ir para simulado) ou `pendente_revisao` (com `motivoPendencia`, ex.: fórmula escrita por extenso, tabela com sobras da conversão, alternativa só com figura); as rotas de simulado vão sortear só as disponíveis. Figuras ficam como imagem Markdown no texto, com a URL original e a descrição como texto alternativo. Sem língua estrangeira, `lingua` é gravada como `''` (e volta `null`), porque `NULL` não conta no `UNIQUE` do Postgres. | Popular o banco de questões dos simulados (ENEM 2022 a 2024, 540 questões) sem perder questões boas por causa de uma ruim, e marcar as que precisam de revisão em vez de descartá-las. |

### Simulados (13/11/2026)

| Código | Decisão | Motivo |
|---|---|---|
| D-80 | **Modelo: simulado = tentativa.** `simulado` guarda a tentativa de um usuário (`banca`, `tamanho`, `cronometrado`, `segundos_por_questao`, `status` EM_ANDAMENTO/CONCLUIDO, `iniciado_em`, `finalizado_em`, `acertos`, `erros`, `em_branco`, `origem_id`), `simulado_area` guarda as áreas escolhidas e `simulado_questao` (chave `simulado_id` + `ordem`) guarda cada questão sorteada com `situacao` (PENDENTE, RESPONDIDA, EM_BRANCO; TEMPO_ESGOTADO saiu na D-86), `alternativa_marcada`, `correta`, `tempo_segundos`, `fora_do_tempo` (D-86) e `atualizada_em`. A mesma questão não repete no mesmo simulado. **Só o ENEM está ativo** (`banca` diferente de `enem`: 400); as 4 áreas são `lc`, `ch`, `cn` e `mt`, sem redação. **Usuário provisório:** o dono é o `UsuarioLogado` (D-55) e `usuario_id` fica sem chave estrangeira; simulado de outro usuário responde **404**. As rotas ficam em `/api/v1/simulados` e não exigem `X-Api-Key`. | Tela de simulados do front (TR-409). O login só troca a classe `UsuarioLogado` e uma migration cria a chave estrangeira. |
| D-81 | **Sorteio.** Só questões `disponivel`, não anuladas e com `lingua` diferente de `ESPANHOL` (hoje só há inglês nas questões 1 a 5 de `lc`). O total do tamanho (`rapida` 10, `media` 20, `longa` 30) é dividido igualmente entre as áreas escolhidas, e **o resto vai para as primeiras na ordem lc, ch, cn, mt** (30 em 4 áreas: 8, 8, 7, 7). Dentro de cada área, entram primeiro as questões que o usuário **nunca teve em nenhum simulado** (qualquer status), em ordem aleatória (embaralhado em Java, portátil entre Postgres e H2), e só depois as repetidas, também embaralhadas. A ordem final é agrupada por área (lc, ch, cn, mt). Se uma área não tiver questões para a sua parte, **409** `Não há questões suficientes de <área> para este simulado`. `GET /simulados/opcoes` informa as questões sorteáveis por área. | Simulado sem repetir questão enquanto houver inédita, e sem misturar língua. |
| D-82 | **Cronômetro por questão e tempo esgotado.** *O tempo escolhido e o fim do bloqueio estão na D-86.* Com `cronometrado`, cada questão tem **150 s** (`segundosPorQuestao`). O app manda em `PUT /simulados/{id}/questoes/{ordem}` o tempo **total acumulado** na questão, e a API **guarda o maior** entre o atual e o recebido. Com tempo cronometrado maior ou igual a 150, a questão vira `tempo_esgotado`, sem alternativa e com `tempo_segundos = 150`; ela fica bloqueada (**409** em qualquer envio novo) e conta como em branco na correção. Simulado já finalizado: **409**. `alternativa` que a questão não tem, `respondida` sem alternativa e `situacao = tempo_esgotado` enviada pelo app: **400** (o esgotamento é decidido só pela API). Sem cronômetro não há limite, mas o tempo continua medido. O relógio é um `java.time.Clock` injetável (bean em `config/RelogioConfig`), para os testes de tempo. | O servidor é quem decide o esgotamento; reenvio fora de ordem não pode diminuir o tempo. |
| D-83 | **Correção só no back e gabarito só na revisão.** `GET /simulados/{id}` e a resposta do `PUT` **nunca** trazem `alternativaCorreta` nem `correta`. `POST /simulados/{id}/finalizar` transforma as pendentes em `em_branco`, grava `correta` por questão e `acertos`, `erros` e `em_branco` (tempo esgotado e em branco contam como em branco) e devolve a revisão, com `alternativaCorreta`, `resultado` (`acerto`, `erro`, `em_branco`), `porcentagemAcerto` (1 casa), `duracaoSegundos` (soma dos tempos) e `porArea`. **Finalizar é idempotente:** a segunda chamada devolve a mesma revisão. `GET /simulados/{id}/revisao` em andamento: **409**. A revisão mostra só o gabarito, sem explicação. | O gabarito não pode vazar antes do fim. |
| D-84 | **Refazer erros.** `POST /simulados/{id}/refazer-erros` cria um simulado novo só com as questões de `resultado = erro`, na ordem original e renumeradas, com `tamanho` nulo, o mesmo `cronometrado` e `origem_id` apontando o simulado de origem. As áreas são as das questões erradas e o título é `Refazer erros · <título do simulado de origem>`. **409** se o simulado não está concluído ou não teve erros. Os itens da lista de simulados trazem `cronometrado`, para o front refazer com a mesma configuração. | Estudar de novo só o que errou. |
| D-85 | **Métricas.** `GET /simulados/resumo` (sobre os concluídos): `simuladosFeitos`, `questoesRespondidas` (situação respondida, em todos os simulados), `aproveitamentoMedio` (acertos de todas as questões dos concluídos, em %, 1 casa; `null` sem concluídos) e `melhorArea` (área de maior % de acerto nos concluídos). A lista (`GET /simulados`, `status`, `ordem` = `recente`, `antigo`, `maior_nota`, `menor_nota`, `page`, `size`, padrão recente e 12) ordena por nota no banco e põe os em andamento no fim. **`GET /simulados/cronometro?periodo=semana, mes ou ano`** (janelas de 7, 30 e 365 dias até hoje, inclusive; anterior = janela vizinha do mesmo tamanho, `null` no ano): tempo de estudo = soma de `tempo_segundos` das respostas pela data de `atualizada_em`; **dia ativo** = dia com ao menos uma resposta (situação diferente de pendente ou tempo maior que 0); `tempoMedioQuestao` = tempo total / questões com tempo maior que 0 (0 se nenhuma); **ofensiva** = dias ativos seguidos até hoje (ou até ontem, se hoje ainda não teve estudo) e **recorde** = maior sequência do último ano. `dias` traz os últimos 365 dias, um item por dia (com zeros); `horasPorSemana` traz S1 a S4 (S4 = últimos 7 dias, valores em segundos); `porConfiguracao` traz um item por tamanho que tenha simulado concluído na janela (`rapida`, `media`, `longa`); `ritmoPorCategoria` traz uma área por item (`lc`, ...). **Os dias são os de `America/Sao_Paulo`** (uma resposta às 23h30 de Brasília, já 02h30 UTC do dia seguinte, conta no dia de Brasília). `atualizada_em` só muda quando o aluno mexe na questão; pendente convertida na finalização não conta como estudo. O tempo da questão vai todo para o dia da última atualização. | Estatísticas da tela Cronômetro (`DadosCronometro` do front) sem o front recalcular nada. |
| D-86 | **Resposta fora do tempo e tempo por questão escolhido** (substitui o bloqueio da D-82). O aluno escolhe o tempo ao criar o simulado cronometrado: `segundosPorQuestao` obrigatório, de **60 a 600, múltiplo de 30** (fora disso, **400** no campo); sem cronômetro o valor é ignorado e fica nulo. `GET /simulados/opcoes` devolve `temposPorQuestao` = `[90, 120, 150, 180, 240]` (o front mostra 1min30, 2min, 2min30, 3min e 4min) e o padrão `segundosPorQuestao` = 150. Refazer erros herda o tempo do simulado de origem. **A questão com o tempo esgotado continua aberta:** o `PUT` não recusa mais por tempo, guarda o tempo real (o maior recebido, sem teto) e, quando grava uma **resposta nova** (situação `respondida` com alternativa diferente da salva) com tempo maior ou igual ao limite, marca `simulado_questao.fora_do_tempo`. Reenviar a mesma resposta não marca; depois de marcada, a questão continua fora do tempo mesmo que a resposta mude. A situação `TEMPO_ESGOTADO` deixou de existir: esgotada sem resposta fica `pendente` e vira `em_branco` ao finalizar (`V2026_11_13_2240__simulado_fora_do_tempo.sql` cria a coluna e converte as linhas antigas: em branco nos concluídos e pendente nos em andamento). **Nota:** a resposta fora do tempo **conta normal** na porcentagem e o selo só avisa; a regra está em um único ponto (`SimuladoService.FORA_DO_TEMPO_CONTA_NA_NOTA`; com `false`, ela vira erro). Contrato: `QuestaoSimulado.foraDoTempo`; na revisão, cada questão traz `foraDoTempo` e `tempoEsgotado` (tempo maior ou igual ao limite, respondida ou não); `RevisaoSimulado` e `SimuladoResumo` trazem `respondidasForaDoTempo`, e `SimuladoResumo` traz `segundosPorQuestao` (para o Refazer da lista). | Pedido do Carlos (TR-409): deixar responder depois do tempo, deixando claro que foi fora do tempo, e deixar o aluno escolher o tempo antes de começar. |

---

### Perfil do usuário (09/11/2026)

| Código | Decisão | Motivo |
|---|---|---|
| D-80 | **Conta do usuário logado em `/api/v1/me`**: `GET` (dados), `PATCH` (nome e nota do ENEM), `PUT /me/foto` (multipart, campo `foto`), `DELETE /me/foto` (204) e `GET /me/foto` (a imagem). **Nenhuma rota recebe id**: o usuário sai do `UsuarioLogado`, então ninguém altera a conta de outra pessoa. Resposta `UsuarioResposta`: `{id, nome, email, emailVerificado, notaEnem, fotoUrl, criadoEm, senhaAlteradaEm}`. Regras do `PATCH`: nome com 3 a 120 caracteres sem contar os espaços das pontas (gravado sem eles); `notaEnem` de 0 a 1000 com até 2 casas, ou `null` para apagar. Erro de campo: **400** com `fields`. Tabela `usuario` criada na migration `V2026_11_09_2345__usuario.sql`, já com o usuário provisório (D-55) e com a **chave estrangeira** `assinatura.usuario_id → usuario.id`. O e-mail é gravado em minúsculas, por isso o `UNIQUE` simples. | Área de Perfil (TR-328) funcionando de ponta a ponta antes do login, sem rota duplicada: o login (D-55) só troca o `UsuarioLogado`. |
| D-81 | **Foto de perfil no próprio Postgres** (`usuario_foto.conteudo BYTEA`), separada do usuário para não ser carregada a cada requisição. Aceita JPG, PNG e WebP **conferidos pelos primeiros bytes** (o `Content-Type` enviado é ignorado), até **2 MB** (`spring.servlet.multipart.max-file-size`; acima disso, **413**). `fotoUrl` = `/api/v1/me/foto?v=<data da troca>`: o `?v=` muda a cada troca, então o `GET /me/foto` pode ser guardado em cache (`Cache-Control: private, max-age=30 dias`). Sem foto: `fotoUrl` `null` e `GET /me/foto` **404**. | Pela D-01 o Supabase é só banco e não há serviço de arquivos; com 2 MB por foto o banco aguenta bem e o deploy não ganha dependência. |
| D-82 | **O e-mail da conta não pode ser trocado** (decisão do Carlos, 11/11/2026): não há rota nem tela para isso; o perfil mostra "O e-mail da conta não pode ser alterado". A **troca de senha** foi publicada na D-91. | Simplifica a conta: o e-mail fica sendo a identidade da pessoa. |

| D-83 | **Usuário de homologação** fixo (`0b5e7c1a-4f2d-4c8e-9a6b-3d1f0e2a7c55`, "Usuária Homologação", `homologacao@sauf.local`, senha `Homolog@2026` gravada como hash BCrypt, nota 720,50) com **Premium ativo até 31/12/2099**, criado pelo script `docs/homologacao/usuario-homologacao.sql`, que **não é migration**. Rodar o script de novo volta a conta ao estado original (perfil, senha, sem foto, uma assinatura). Entra-se com ele pela tela de login (D-84). | Testar as telas de assinante com dados que não vencem nem mudam, sem levar dados de teste para o Supabase por migration. |

### Login e cadastro (10/11/2026)

Implementam a D-55 e a ADR-005 do front. Fecham a P-09 e a P-10.

| Código | Decisão | Motivo |
|---|---|---|
| D-84 | **Sessão = JWT HS256 em cookie `sauf_sessao`** (`HttpOnly`, `SameSite=Lax`, `Path=/api`, `Secure` com `SAUF_COOKIE_SECURE=true`), criado no login e no cadastro e apagado no logout (`Max-Age=0`). O token leva o id do usuário em `sub` e vale **7 dias** (`SAUF_SESSAO_DIAS`); depois disso a pessoa entra de novo. Segredo em `SAUF_JWT_SECRET` (32+ bytes em base64, **obrigatório**: sem ele a API não sobe). Sem sessão no servidor (Spring Security `STATELESS`); o `FiltroSessao` lê o cookie e o `UsuarioLogado` devolve o usuário dele. Token vencido ou adulterado = visitante (rotas públicas seguem funcionando). Sem CSRF token: cookie `SameSite=Lax` + corpos só JSON/multipart + CORS só para o front. Senhas em **BCrypt**. Login errado: **401** com a mensagem única **"E-mail ou senha incorretos"** (não revela se o e-mail existe), sem bloqueio por tentativas por enquanto. Contas sem senha (o usuário de teste da D-55) não entram. Rotas: `POST /api/v1/auth/login` `{email, senha}` → 200 + cookie + `UsuarioResposta`; `POST /api/v1/auth/logout` → 204 (público, funciona com a sessão vencida). | Login próprio, como decidido na D-55; token fora do alcance do JavaScript (ADR-005). |
| D-85 | **Cadastro em `POST /api/v1/auth/cadastro`** `{nome, email, senha, aceites}` → **201** + cookie (a conta **já entra**) + `UsuarioResposta`. E-mail gravado em minúsculas e sem os espaços das pontas; e-mail já usado: **409** com `fields.email`. Grava `senha_alterada_em` e `termos_aceitos_em` (migration `V2026_11_10_0040__login.sql`); `emailVerificado` nasce `false` (a verificação ficou para depois, D-89). Os aceites dos documentos legais seguem a D-93. | As telas de cadastro já existiam; a pessoa já sai logada. |
| D-86 | **Regra da senha**: de 8 a 72 caracteres, com **pelo menos 1 letra e 1 caractere especial** (o que não é letra, número nem espaço). Fica num lugar só na API (`@SenhaValida`), com a mensagem do primeiro requisito que falhar em `fields.senha`; o front repete as mesmas regras e mostra a lista ao digitar. 72 é o limite do BCrypt. | Pedido do Carlos (10/11/2026). |
| D-87 | **Exigem login** (401 "Entre na sua conta para continuar" sem sessão): `/api/v1/me/**` e `/api/v1/assinatura/**`, menos `GET /api/v1/assinatura/planos`. O catálogo, as áreas, o Swagger e o login/cadastro são públicos; a ingestão segue com a `X-Api-Key`. Novas rotas da conta (ex.: favoritos) entram na lista do `SegurancaConfig`. **O usuário provisório saiu do código** (o `UsuarioLogado` só lê a sessão); o usuário de teste continua no banco, sem senha. | Proteger só o que é da conta, sem quebrar as telas públicas. |

### E-mail, confirmação e senha (11/11/2026)

| Código | Decisão | Motivo |
|---|---|---|
| D-88 | **E-mails por SMTP** (`spring-boot-starter-mail`, interface `EnvioEmail`). Local: **Mailpit** no `docker-compose.yml` (SMTP em `localhost:1025`, caixa de entrada em **http://localhost:8025**; nada sai para a internet). Envio real: **Gmail com "senha de app"** (decisão do Carlos, 14/11/2026) ou outro SMTP, pelas variáveis `SAUF_MAIL_HOST`, `SAUF_MAIL_PORT`, `SAUF_MAIL_USER`, `SAUF_MAIL_PASSWORD`, `SAUF_MAIL_AUTH`, `SAUF_MAIL_STARTTLS` e `SAUF_MAIL_FROM` (ver README). E-mails em português, texto puro + HTML simples. **Falha de envio não derruba a requisição**: vai para o log e a pessoa pede de novo. Nos testes, uma caixa em memória (`CaixaDeEmailsTeste`). | Dá para desenvolver sem credenciais; o envio real é só configuração, sem mudar código. |
| D-89 | **Verificação do e-mail: adiada** (decisão do Carlos, 11/11/2026). A ideia do time é só permitir o cadastro com e-mail verificado. Não existe forma confiável de saber se uma caixa de e-mail existe sem mandar uma mensagem para ela (formato e registro MX do domínio não provam que a caixa existe nem que é da pessoa; servidores como Gmail não respondem de propósito; serviços pagos também não provam a posse). Proposta para quando for feita: **código de 6 dígitos no meio do cadastro** (dados → código enviado por e-mail, 10 min, até 5 tentativas → termos) e a conta só é criada depois do código certo, já verificada; mais a checagem do domínio (MX) para pegar erros como "gmial.com". Até lá, a conta entra sem verificar e o campo `emailVerificado` só vira `true` pela redefinição de senha (D-90). | Não atrapalhar o cadastro agora; decidir o formato com o time. |
| D-90 | **Esqueci minha senha por código** (revisada em 14/11/2026, pedido do Carlos; antes era por link). `POST /api/v1/auth/esqueci-senha` `{email}` responde **sempre 204** (não revela quais e-mails têm conta) e manda um **código de 6 dígitos** (vale **10 minutos**, uso único, no máximo **5 tentativas**, um e-mail por minuto; pedir outro invalida o anterior). `POST /api/v1/auth/esqueci-senha/codigo` `{email, codigo}`: código certo → **200 + cookie** (a pessoa entra) + `UsuarioResposta` com `trocaSenhaSemAtualAte` (agora + **15 minutos**). Errado: **400** em `fields.codigo` com as tentativas restantes; vencido, usado, 5 erros ou e-mail sem conta: **400** "Código inválido ou vencido. Peça um novo código." Receber o código prova o e-mail, então ele fica **verificado**. Os códigos ficam em `token_email` só como **hash BCrypt** (migration `V2026_11_14_0300__codigo_acesso.sql`, que também conta as tentativas e invalidou os links antigos). O front leva a pessoa ao perfil com "Criar nova senha" aberto. | Recuperar o acesso digitando o código na própria tela e trocar a senha no perfil. |
| D-91 | **Troca de senha no perfil**: `PUT /api/v1/me/senha` `{senhaAtual, novaSenha}` → 204 (exige login). Senha atual errada (ou conta sem senha): **400** `fields.senhaAtual` "Senha atual incorreta"; sem a senha atual: "Informe sua senha atual"; nova igual à atual: **400** `fields.novaSenha`; regra da D-86 em `fields.novaSenha`. **Exceção**: até 15 minutos depois de entrar pelo código (D-90; a sessão leva `troca_senha_ate` no JWT) a senha atual **não é pedida**; feita a troca, a API renova o cookie sem essa liberação. `GET /me` informa a liberação em `trocaSenhaSemAtualAte`. Atualiza `senha_alterada_em`. | Pedido do Carlos; o e-mail não muda (D-82). |
| D-92 | **Erros de conexão e de servidor no login e na conta** (14/11/2026, pedido do Carlos). API: banco fora do ar responde **503** "O SAUF está temporariamente indisponível…" em até **5 s** (`spring.datasource.hikari.connection-timeout`), em vez de segurar a requisição por 30 s. Front (`lib/api/erros.ts`): sem internet, API fora do ar, tempo esgotado (15 s), 503 e outros 5xx viram mensagens prontas; consultas tentam de novo sozinhas até 2 vezes. Se não der para consultar a conta (`GET /me` falhou), o perfil e as telas protegidas mostram "Não foi possível carregar sua conta" com **Tentar de novo**, em vez de tratar a pessoa como visitante ou mandá-la ao login. **401 numa rota da conta = sessão expirada**: o front passa a tratar como visitante e o login avisa "Sua sessão expirou" (o 401 do próprio login continua sendo senha errada). | A pessoa entende o que aconteceu e o que fazer, sem ficar presa numa tela vazia ou ser deslogada sem explicação. |
| D-93 | **Documentos legais versionados (Termos de Uso e Política de Privacidade) e aceites** (15/11/2026, pedido do Carlos; baseado no rascunho `V__termos_e_aceites.sql` dele, sem a parte de administração). **Fonte**: arquivos `.md` em `src/main/resources/documentos-legais/`, um por versão, com cabeçalho (`tipo`, `versao` semver, `vigente_a_partir_de`, `exige_novo_aceite`, `resumo_alteracoes` opcional). **Ao subir**, a API copia cada versão para `documento_legal_versao` com o texto e o **SHA-256**; versão que já tem aceites **não pode mudar** (a API não sobe e pede uma versão nova); sem aceites, pode ser corrigida. **Vigente** = maior versão cuja data já chegou (dá para publicar com antecedência). `GET /api/v1/documentos-legais` (público) devolve as vigentes com `versao`, `titulo`, `vigenteDesde`, `minutosLeitura`, `resumoAlteracoes`, `conteudo` (Markdown) e `sha256`. **Cadastro** manda `aceites: [{tipo, versao}]` dos dois documentos (no lugar do antigo `aceitouTermos`); faltando um: **400** `fields.aceites`; versão que não é a vigente: **409** "Os documentos foram atualizados enquanto você lia…" e a conta não é criada. Cada aceite vira uma linha em `aceite_documento` (usuário, versão, hash, data e hora, **IP** — primeiro item do `X-Forwarded-For` atrás de proxy — navegador e canal `WEB`), só inserção. **Versão nova com `exige_novo_aceite: true`**: `GET /me` traz `documentosPendentes` e o front mostra "Atualizamos nossos termos" até a pessoa aceitar em `POST /api/v1/me/aceites` `{aceites: [...]}` (ou sair). Pendente = não aceitou a última versão vigente que exige aceite, nem uma posterior. Migration `V2026_11_15_0300__documentos_legais.sql` (também removeu `usuario.termos_aceitos_em`: o aceite do texto fictício antigo não vale). O **Termo de Consentimento** (autorizações opcionais) e a **área administrativa** ficaram para depois. **Pendente de conteúdo**: os campos `[NOME OU RAZÃO SOCIAL DO CONTROLADOR]`, `[CPF/CNPJ]` etc. dos textos (preencher gera a versão 1.0.1, ou corrige a 1.0.0 enquanto ninguém de verdade aceitou). | Guardar exatamente o que cada pessoa aceitou e quando (LGPD), com publicação simples: editar um arquivo e fazer o deploy. |
| D-94 | **E-mail já cadastrado é avisado antes da etapa dos termos** (15/11/2026, pedido do Carlos). `POST /api/v1/auth/email-disponivel` `{email}` → `{disponivel: true|false}` (sem diferenciar maiúsculas; e-mail inválido: 400). O front chama ao clicar em "Continuar" na etapa 1 e, se o e-mail já tem conta, mostra no campo "Já existe uma conta com este e-mail. Entre na sua conta ou use Esqueci minha senha." sem ir para os termos. POST com o e-mail no corpo, para ele não aparecer em URL nem em log. O 409 do cadastro continua como garantia (duas pessoas ao mesmo tempo). Essa rota revela se um e-mail tem conta, como o próprio cadastro já revelava pelo 409; o login e o "esqueci minha senha" continuam sem revelar. | A pessoa descobre na hora, sem ler os termos à toa. |
| D-95 | **Tempo por questão só como referência** (15/11/2026, pedido do Carlos; substitui a parte de "fora do tempo" da D-86). Passar do tempo escolhido não muda nada: o `PUT` guarda o tempo real e a resposta, sem marca. Saíram do contrato `QuestaoSimulado.foraDoTempo`, `QuestaoRevisao.tempoEsgotado` e `foraDoTempo`, `RevisaoSimulado.respondidasForaDoTempo` e `SimuladoResumo.respondidasForaDoTempo`, e a constante `FORA_DO_TEMPO_CONTA_NA_NOTA`. A coluna `simulado_questao.fora_do_tempo` fica no banco, sem uso (`V2026_11_15_1745__em_branco_escolhido.sql`). **Modo do temporizador:** `cronometrado = true` é o **regressivo** (conta para baixo a partir de `segundosPorQuestao`); `cronometrado = false` é o **crescente** (conta para cima o tempo de cada questão, sem limite). O front mostra os dois nomes e um **tempo total** do simulado (soma dos tempos das questões); o contrato não mudou. | O cronômetro serve para o aluno se orientar, não para punir. Reaproveitar o `cronometrado` evita migration e mantém os simulados já gravados. |
| D-96 | **Em branco é só o que o aluno escolheu e refazer leva os em branco** (15/11/2026, pedido do Carlos; muda a D-84). Ao finalizar, a questão **pendente continua pendente** (antes virava `em_branco`); `em_branco` é só quem clicou em "Deixar em branco". O `resultado` das duas continua `em_branco` e elas contam igual em `emBranco` e na nota. Na revisão, cada questão traz `deixadaEmBranco` (true só na escolhida), que o front pinta de amarelo. `POST /simulados/{id}/refazer-erros` agora leva **as erradas e as em branco (escolhidas ou sem resposta)**, na ordem original; sem nenhuma delas: **409**. Título do novo: "Refazer erros e em branco · ...". Simulados concluídos antes disso: a migration `V2026_11_15_1745` volta para `pendente` a questão nunca tocada (`atualizada_em` nula); a que foi aberta e ficou sem resposta não dá para separar e continua `em_branco`. | Questão em branco não é erro, mas precisa ser refeita junto. |
| D-97 | **Simulado personalizado: quantidade livre ou por área** (15/11/2026, pedido do Carlos; amplia a D-81 sem mudar os tamanhos prontos). O `POST /api/v1/simulados` aceita, além de `tamanho` + `areas`, dois campos opcionais. **`quantidade`** (1 a 100): total livre dividido entre as `areas` como na D-81 (o resto vai para as primeiras na ordem lc, ch, cn, mt). **`quantidades`** (`{"lc": 5, "cn": 10, "mt": 3}`): quantas questões de cada área; o total é a soma e as `areas` passam a ser as que têm mais de 0. **Precedência**: `quantidades` > `quantidade` > `tamanho` (os outros campos são ignorados). **Validação (400 com `fields`)**: área desconhecida (`fields.quantidades` "Área desconhecida: xx"), número negativo (`fields["quantidades.<área>"]`), área acima das sorteáveis (`fields["quantidades.<área>"]` "Linguagens tem N questões disponíveis"), soma 0 ("Escolha ao menos 1 questão"), soma acima de 100 ("O simulado pode ter até 100 questões (você escolheu N)"), `quantidade` fora de 1 a 100 (`fields.quantidade`), nenhum dos três (`fields.tamanho`) e sem áreas fora do modo por área (`fields.areas`). Com `quantidade`, se a parte de uma área passar do que ela tem, continua o **409** da D-81. O sorteio não repete questão no simulado e prefere as inéditas (D-81). **Gravação**: simulado personalizado fica com `tamanho` nulo (como o refazer da D-84) e as áreas usadas em `simulado_area`; a distribuição sai das próprias questões, sem migration. `GET /simulados/opcoes` ganhou `maximoQuestoes` (100) e cada item de `GET /simulados` ganhou `questoesPorArea` (`{"lc": 5, ...}`, só as áreas presentes), para o front repetir um personalizado igual. A tela Cronômetro (`porConfiguracao`, D-85) continua só com os tamanhos prontos; personalizados não entram nesse quadro. | Quem quer treinar mais (ou só uma área) monta o simulado do seu jeito, e o contrato antigo continua valendo. |
| — | **Ficaram fora por decisão do Carlos**: limite de tentativas no login e exclusão da conta (11/11/2026); **login com Google não será feito** (14/11/2026, o botão saiu da tela de login). | — |

### Dados manuais das universidades e imagens (20/11/2026, TR-343)

| Código | Decisão | Motivo |
|---|---|---|
| D-98 | **Dados que nenhuma fonte oficial entrega são cadastrados à mão em arquivos versionados no `sauf-scraper`** (pedido do Carlos, 20/11/2026): `dados/ies/<sigla>.toml` (ano de fundação, "sobre", site, logo e capa) e `dados/areas.toml` (uma imagem por área Cine), com as imagens ao lado, em `img/`. Formato **TOML**, e não o `.yaml` citado na D-51: o `tomllib` já vem com o Python (sem pacote novo) e aceita texto de várias linhas. O scraper valida (Pydantic) e envia com `uv run sauf-scraper ies [--sigla X] [--dry-run]` e `uv run sauf-scraper areas [--codigo 07] [--dry-run]`, pela ingestão com a `X-Api-Key` (sem papel de administrador por enquanto). Rotas: **`PUT /api/v1/ingestao/universidades/{codigoEmec}`** `{anoFundacao, sobre: [parágrafos], siteUrl, logo, capa}` **substitui o bloco inteiro** (logo ou capa `null` apaga a imagem) e é idempotente: `atualizados` = 1 quando algo mudou, 0 num reenvio igual; **`PUT /api/v1/ingestao/areas/{codigoCine}/imagem`** `{conteudoBase64, autor, licenca, licencaUrl, fonteUrl}`. Validação: `anoFundacao` de 1500 até o ano atual; `sobre` com 1 a 5 parágrafos de até 2000 caracteres, sem linha em branco dentro (`fields["sobre[i]"]`); `siteUrl` com http(s). IES ou área que não existe no banco: **422** (mesma regra do lote, D-27 e ADR-BE-008), não 404. Passo a passo para quem preenche: `sauf-scraper/dados/README.md`. Decisão completa: ADR-BE-017. | Dados que mudam pouco e precisam de alguém para escolher e conferir ficam no Git, com histórico e revisão, e entram pelo mesmo caminho dos outros importadores. |
| D-99 | **Imagens guardadas no Postgres (`BYTEA`), sem bucket**: tabelas `instituicao_imagem` (chave instituição + `LOGO`/`CAPA`) e `area_imagem` (migration `V2026_11_20_1200__dados_manuais_ies.sql`), com o conteúdo, o tipo, o hash SHA-256 e o crédito. Mesmas regras da foto do perfil (D-81, `comum/TipoImagem`): **JPG, PNG ou WebP conferidos pelo conteúdo**, até **2 MB**, base64 estrito; fora disso, **400** com o campo em `fields`. Leitura pública: `GET /api/v1/universidades/{id}/logo`, `/capa` e `GET /api/v1/areas/{id}/imagem`, com `ETag` (o hash), `Cache-Control: public, max-age=604800` e **304** quando o navegador já tem a versão; **404** sem imagem. As respostas trazem a URL completa com `?v=<12 primeiros caracteres do hash>`, que muda quando a imagem muda: `logoUrl` na listagem, no detalhe e nos favoritos, `capaUrl` no detalhe. A coluna antiga `instituicao.logo_url` ficou sem uso. | São poucas imagens pequenas (cerca de 20); um banco só, sem pacote nem serviço novo, como a foto do perfil. |
| D-100 | **Logo, capa e imagens das áreas vêm do Wikimedia Commons, com a licença registrada.** Cada imagem guarda `autor`, `licenca`, `licencaUrl` (opcional, sem link em domínio público) e `fonteUrl` (a página do arquivo no Commons; o scraper recusa outro endereço). Preferência por CC0 e domínio público; CC BY e CC BY-SA são aceitas, com crédito ao autor; logos sem licença livre clara não são usados. O detalhe da universidade devolve `creditoLogo` e `creditoCapa` (`{autor, licenca, licencaUrl, fonteUrl}`, `null` sem imagem); o front mostra uma **legenda pequena "Foto: autor, licença" com link para a página do arquivo** (decidido pelo Carlos em 20/11/2026). As imagens das áreas são CC0 e não pedem crédito. | Usar só imagens que podemos publicar, com prova de onde vieram. |
| D-101 | **Imagem dos cursos = uma imagem por área Cine, por enquanto** (decisão do Carlos, 20/11/2026). O `imagemUrl` do card do catálogo (`CursoResumo`) é a imagem própria do curso quando existir (hoje nenhum tem) e, se não, a da área do curso; curso sem área fica `null`. | Uma imagem por curso daria mais de 100 escolhas manuais; por área são 11. |
| D-102 | **Ano de fundação = ano da criação legal** (decisão do Carlos, 20/11/2026): o ato (lei ou decreto) que criou a instituição com a forma atual, como a própria universidade apresenta no site oficial. A instituição de origem entra no "sobre", não no ano (ex.: UTFPR = 2005, Lei 11.184; o "sobre" conta a história desde 1909). Cadastrados: UEM 1969 (Lei Estadual 6.034/1969), UEL 1970 (Decreto Estadual 18.110/1970), UTFPR 2005 (Lei 11.184/2005), UFSC 1960 (Lei 3.849/1960), UFSM 1960 (Lei 3.834-C/1960). O "sobre" é escrito pelo time a partir dos sites oficiais (2 a 3 parágrafos, links em `[fontes]` no arquivo) e revisado antes de ir para produção. | Um critério só, com documento oficial, para as universidades poderem ser comparadas. |
| D-103 | **O Censo e os spiders nunca sobrescrevem os dados manuais**: ano de fundação, "sobre", site e imagens só mudam pelas rotas da D-98 (há teste reimportando o Censo depois do envio). `instituicao.dados_manuais_em` guarda quando o ano, o "sobre" ou o site mudaram pela última vez (cada imagem tem o próprio `atualizada_em`). | Rodar os importadores de novo não pode apagar o trabalho feito à mão. |

### Calendário e administração (20/11/2026, TR-343)

| Código | Decisão | Motivo |
|---|---|---|
| D-104 | **Calendário de vestibulares na tabela `evento_calendario`** (aprovado pelo Carlos em 20/11/2026; fecha a P-33). Cada linha é um evento de **um dia** (`fim` nulo) ou um **período** (`inicio` a `fim`), com `tipo` `inscricao`, `prova`, `resultado`, `feira` ou `outro` (os 4 do protótipo mais `outro`, para gabaritos e confirmação de vaga), `titulo`, `descricao` opcional, `fonte_url` obrigatória (a página oficial da data), `ano_referencia` (o ano da edição: 2027 em "Vestibular 2027") e `instituicao_id` opcional (nulo = evento nacional, como ENEM e ProUni). A **`chave`** (única, ex.: `enem-2026-prova-1`, só minúsculas, números e hífen) é o nome estável do evento. Começamos só com ENEM, os vestibulares das 5 IES com scraper e o ProUni; **data que a fonte ainda não publicou não entra** ("a divulgar"). | Um modelo só para datas nacionais e das universidades, sempre com a fonte, e sem inventar data. |
| D-105 | **Leitura pública**: `GET /api/v1/calendario/eventos?ano=` devolve os eventos com algum dia no ano (um período que cruza o ano aparece nos dois), no formato do front (`id`, `titulo`, `tipo`, `data` = início) mais `chave`, `inicio`, `fim`, `descricao`, `fonteUrl`, `anoReferencia` e `instituicao {id, sigla, nome}`. `GET /api/v1/calendario/proximos-prazos?limite=` (padrão 3, até 20) devolve os eventos cujo **prazo** (o `fim` do período ou o dia do evento) ainda não passou, do mais próximo ao mais distante, com `data` = prazo e `descricao` = quanto falta ("Encerra em 5 dias", "Amanhã", "Começa em 10 dias"). **"Hoje" é o dia em Brasília** (`America/Sao_Paulo`). O `proximoPrazo` do detalhe da universidade passa a vir do calendário (`titulo`, `data` = prazo, `editalUrl` = `fonteUrl`), `null` sem evento por vir. Para as **notificações** (feitas por um colega), o `CalendarioService` expõe `proximosEventos(ate)` e `eventosEntre(de, ate)`: só consulta, não avisa ninguém. | O front já espera essas duas rotas; os prazos precisam de uma regra só para Home, detalhe e notificações. |
| D-106 | **Escrita pelas rotas de admin**: `PUT /api/v1/admin/calendario/eventos/{chave}` cria ou substitui o evento (reenviar igual responde sem `criados` nem `atualizados`; `codigoEmecInstituicao` desconhecido: **422**; `fim` antes do `inicio`, chave ou corpo inválido: **400**) e `DELETE` na mesma rota apaga (**204**; **404** se não existe). Hoje quem envia é o `sauf-scraper calendario` (arquivo `dados/calendario.toml`, ADR-BE-017); as telas de admin virão depois e usam as mesmas rotas. | Mesmo jeito dos dados manuais: o arquivo é a fonte, o envio é idempotente, e as telas futuras não precisam de outra API. |
| D-107 | **Papel do usuário e acesso de admin.** `usuario.papel` (`ESTUDANTE` por padrão ou `ADMIN`). As rotas **`/api/v1/admin/**`** aceitam a **`X-Api-Key`** (o scraper) **ou** uma sessão de conta **ADMIN** (as telas futuras); sem nenhum dos dois: **401**; logado sem ser admin: **403**. A variável **`SAUF_ADMIN_EMAILS`** (e-mails separados por vírgula, sem diferenciar maiúsculas) promove a conta a ADMIN na primeira vez que ela usa uma rota de admin; tirar o e-mail da lista **não rebaixa** (trocar o papel no banco). Cada ambiente tem a sua lista no próprio `.env`; o repositório só tem um exemplo no `.env.example`. A promoção vai para o log só com o id da conta. | Administrar sem tela de cadastro de admins e sem e-mail de ninguém no código. |

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
- **Banco de questões** (D-68, `V2026_11_08_0945__banco_questoes.sql`):
    - **`prova`**: `id`, `banca` (ENEM/UEM), `ano`, `edicao`, `caderno`, `url_origem`, `credito`,
      `atualizada_em`. Única por (`banca`, `ano`, `edicao`, `caderno`).
    - **`texto_base`**: `id`, `prova_id`, `conteudo`, `hash` (único por prova).
    - **`questao`**: `id`, `prova_id`, `texto_base_id`, `numero`, `lingua`, `tipo`, `area`,
      `materia`, `assunto`, `habilidade`, `comando`, `gabarito_soma`, `dificuldade` (parâmetro B
      da TRI), `anulada`, `status`, `motivo_pendencia`, `codigo_origem` (CO_ITEM do INEP),
      `credito`, `atualizada_em`. Única por (`prova_id`, `numero`, `lingua`).
    - **`alternativa`**: (`questao_id`, `rotulo`) + `valor` (somatória), `texto`, `correta`.
- **Usuário** (D-80 a D-82, `V2026_11_09_2345__usuario.sql`):
    - **`usuario`**: `id`, `nome`, `email` (minúsculas, único), `senha_hash` (preparado, D-82),
      `email_verificado`, `nota_enem`, `senha_alterada_em`, `criado_em`. Já tem o usuário
      provisório (D-55). `assinatura.usuario_id` aponta para ela.
    - **`usuario_foto`**: `usuario_id` (PK, apagada junto com o usuário), `conteudo` (BYTEA),
      `tipo`, `atualizada_em`.
    - `usuario.papel` (`ESTUDANTE` ou `ADMIN`, D-107; `V2026_11_20_1500__calendario_admin.sql`).
- **`evento_calendario`** (D-104, `V2026_11_20_1500__calendario_admin.sql`): `id`, `chave` (única),
  `titulo`, `tipo`, `inicio`, `fim` (nulo = um dia), `instituicao_id` (nulo = nacional),
  `descricao`, `fonte_url`, `ano_referencia`, `atualizado_em`.

- **Simulados** (D-80, `V2026_11_13_1908__simulados.sql`):
    - **`simulado`**: `id`, `usuario_id` (sem chave estrangeira, D-55), `banca`, `tamanho` (nulo no
      refazer erros), `cronometrado`, `segundos_por_questao`, `status`, `iniciado_em`,
      `finalizado_em`, `acertos`, `erros`, `em_branco` (nulos até finalizar), `origem_id`.
    - **`simulado_area`**: (`simulado_id`, `area`).
    - **`simulado_questao`**: (`simulado_id`, `ordem`) + `questao_id` (única por simulado),
      `situacao`, `alternativa_marcada`, `correta`, `tempo_segundos`, `fora_do_tempo` (D-86), `atualizada_em`.

### Rotas da v1

| Rota | Uso no front |
|---|---|
| `GET /api/v1/cursos` | Listagem do catálogo (tela de cursos), paginada, com filtros `area`, `modalidade`, `grau`, `duracaoSemestres`, `uf`, `cidade`, `busca`. Cada item traz `totalUniversidades`, `modalidades`, `graus`, duração mínima/máxima em semestres e a menor nota de corte, calculados só com as ofertas que passam nos filtros. Sem filtro de nota de corte por enquanto (ver seção 4). |
| `GET /api/v1/cursos/{id}` | Detalhe do curso genérico (aceita `uf`/`cidade` para os números da região). |
| `GET /api/v1/cursos/{id}/ofertas` | Universidades que ofertam o curso, uma por página do site, com `opcoes` (`grau` + `turno`), o texto `sobre` (D-37) e o link "acessar o site". Filtros `modalidade`, `grau`, `uf`, `cidade`. |
| `GET /api/v1/universidades` | Listagem de universidades, paginada (12 por página, por nome), com filtros `busca`, `uf`, `cidade`, `tipo`, `modalidade` e `notaMec` (D-59 a D-61). |
| `GET /api/v1/universidades/contagens` | Contadores dos filtros de tipo e modalidade, com os mesmos filtros da listagem (busca facetada, D-62). |
| `GET /api/v1/universidades/cidades` | Cidades do estado (`uf`) com universidade ativa, para o select de cidade (D-63). |
| `GET /api/v1/universidades/{id}` | Detalhe da universidade (visão geral), com `esfera`, `enderecoSede`, cursos por área, `vagasPorAno`, `gratuita` e as 3 `modalidades` (D-58, D-64 a D-67). |
| `GET /api/v1/universidades/{id}/ofertas` | Cursos ofertados pela universidade (filtros `area`, `modalidade`, `grau`). |
| `GET /api/v1/areas` | Áreas para os filtros, com contagem de cursos. |
| `GET /api/v1/calendario/eventos?ano=` | Tela de calendário: eventos com algum dia no ano (padrão: o ano atual), com `data` = início, `inicio`, `fim`, `fonteUrl` e a universidade (D-104, D-105). |
| `GET /api/v1/calendario/proximos-prazos?limite=` | Prazos da Home: próximos eventos pelo prazo (fim do período ou o dia), com `descricao` "Encerra em N dias" (D-105). |
| `PUT /api/v1/admin/calendario/eventos/{chave}` | Cria ou substitui um evento (`sauf-scraper calendario`, futuras telas de admin). IES desconhecida: **422**. Exige `X-Api-Key` ou sessão ADMIN (D-106, D-107). |
| `DELETE /api/v1/admin/calendario/eventos/{chave}` | Apaga um evento (**204**; **404** se não existe). Exige `X-Api-Key` ou sessão ADMIN (D-106, D-107). |
| `POST /api/v1/ingestao/censo` | Import do Censo (`sauf-scraper censo`). Cria/atualiza instituições pelo código e-MEC, cursos genéricos pelo rótulo Cine (D-31, D-36) e a tabela `censo_curso`. **Não cria ofertas** (D-33); depois do import, as ofertas do site já existentes são completadas de novo. Exige `X-Api-Key`. |
| `POST /api/v1/ingestao/lotes` | Lote do scraper. Cria/atualiza uma oferta por página pela `chave` (D-34), com `opcoes` e `codigosEmec`, e completa com o Censo (D-35). Instituição não cadastrada: **422**. Exige `X-Api-Key`. |
| `POST /api/v1/ingestao/questoes` | Lote de questões de uma prova (`sauf-scraper questoes enem --ano ...`). Upsert por prova e por número + língua; recusa só a questão inválida (D-68). Exige `X-Api-Key`. |
| `PUT /api/v1/ingestao/universidades/{codigoEmec}` | Dados manuais da universidade (`sauf-scraper ies`): ano de fundação, "sobre", site, logo e capa. Substitui o bloco; IES não cadastrada: **422**; imagem inválida: **400** (D-98 a D-103). Exige `X-Api-Key`. |
| `PUT /api/v1/ingestao/areas/{codigoCine}/imagem` | Imagem da área Cine, usada nos cursos da área (`sauf-scraper areas`). Área não cadastrada: **422** (D-98, D-101). Exige `X-Api-Key`. |
| `GET /api/v1/universidades/{id}/logo` e `/capa` | Logo e capa da universidade, com `ETag` e cache de 7 dias; **404** sem imagem (D-99). |
| `GET /api/v1/areas/{id}/imagem` | Imagem da área Cine, com `ETag` e cache de 7 dias; **404** sem imagem (D-99, D-101). |
| `GET /api/v1/simulados/opcoes` | Tamanhos, máximo de questões do personalizado, tempo por questão (padrão e opções), bancas e áreas com as questões sorteáveis (D-81, D-86, D-97). |
| `POST /api/v1/simulados` | Cria o simulado e sorteia as questões, por `tamanho`, `quantidade` livre (1 a 100) ou `quantidades` por área. 400 sem áreas, banca inválida, tempo por questão fora da regra ou quantidades inválidas; 409 sem questões suficientes (D-80, D-81, D-86, D-97). |
| `GET /api/v1/simulados` | Lista paginada (12 por página) com `status` e `ordem` (`recente`, `antigo`, `maior_nota`, `menor_nota`) (D-85). |
| `GET /api/v1/simulados/{id}` | Simulado com as questões, nunca com gabarito (D-83). |
| `PUT /api/v1/simulados/{id}/questoes/{ordem}` | Registra resposta e tempo acumulado; o tempo por questão é só referência (D-82, D-86, D-95). |
| `POST /api/v1/simulados/{id}/finalizar` | Corrige e devolve a revisão; idempotente (D-83). |
| `GET /api/v1/simulados/{id}/revisao` | Revisão com gabarito; 409 em andamento (D-83). |
| `POST /api/v1/simulados/{id}/refazer-erros` | Novo simulado com as questões erradas e as em branco (D-84, D-96). |
| `GET /api/v1/simulados/resumo` | Simulados feitos, questões respondidas, aproveitamento médio e melhor área (D-85). |
| `GET /api/v1/simulados/cronometro` | Estatísticas de tempo de estudo, `periodo` = `semana`, `mes` ou `ano` (D-85). |
| `GET /api/v1/assinatura/planos` | Planos da tela de assinatura, do mais barato ao mais caro (D-54). |
| `GET /api/v1/assinatura` | Assinatura vigente do usuário: `{plano, status, renovaEm}`, ou **204** se não houver (D-56). |
| `PUT /api/v1/assinatura` | Assina ou troca de plano. Corpo `{ "plano": "plus" }`. Plano inexistente: **400**; o mesmo plano já ativo: **409**. |
| `POST /api/v1/assinatura/cancelar` | Agenda o cancelamento; o acesso continua até o `renovaEm`. Sem assinatura ou já agendado: **409**. |
| `POST /api/v1/assinatura/reativar` | Desfaz o cancelamento agendado. Se não estiver agendado: **409**. |
| `POST /api/v1/auth/cadastro` | Cria a conta e já entra (cookie), com o aceite dos documentos legais vigentes. E-mail já usado ou documento desatualizado: **409**; senha fora da regra ou documento faltando: **400** (D-85, D-86, D-93). |
| `POST /api/v1/auth/email-disponivel` | Confere, antes da etapa dos termos, se o e-mail já tem conta: `{disponivel}` (D-94). |
| `GET /api/v1/documentos-legais` | Termos de Uso e Política de Privacidade vigentes, com versão, data e texto em Markdown (D-93). |
| `POST /api/v1/me/aceites` | Aceite de uma versão nova dos documentos (tela "Atualizamos nossos termos"). **204**; versão desatualizada: **409** (D-93). |
| `POST /api/v1/auth/login` | Entra (cookie de 7 dias). Errado: **401** "E-mail ou senha incorretos" (D-84). |
| `POST /api/v1/auth/logout` | Sai: apaga o cookie (**204**). |
| `POST /api/v1/auth/esqueci-senha` | Manda o código de 6 dígitos por e-mail. Sempre **204** (D-90). |
| `POST /api/v1/auth/esqueci-senha/codigo` | Código certo: entra (cookie) e libera criar a senha nova sem a atual por 15 min. Errado/vencido: **400** (D-90). |
| `PUT /api/v1/me/senha` | Troca a senha no perfil (**204**). Senha atual errada: **400** em `fields.senhaAtual`; dispensada logo depois de entrar pelo código (D-90, D-91). |
| `GET /api/v1/me` | Dados do usuário logado (tela de perfil, Header): `UsuarioResposta` (D-80). |
| `PATCH /api/v1/me` | Edita nome e nota do ENEM. Corpo `{ "nome": "…", "notaEnem": 698.5 }`. Campo inválido: **400** com `fields` (D-80). |
| `PUT /api/v1/me/foto` | Envia ou troca a foto (multipart, campo `foto`; JPG, PNG ou WebP até 2 MB). Devolve `UsuarioResposta` com a nova `fotoUrl`. Não é imagem: **400**; maior que 2 MB: **413** (D-81). |
| `DELETE /api/v1/me/foto` | Remove a foto. **204**, mesmo sem foto (D-81). |
| `GET /api/v1/me/foto` | A imagem, com o `Content-Type` dela. Sem foto: **404** (D-81). |

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
Banco de questões: `V2026_11_08_0945__banco_questoes.sql` (D-68).
Simulados: `V2026_11_13_1908__simulados.sql` (D-80) e `V2026_11_13_2240__simulado_fora_do_tempo.sql` (D-86).

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
| P-11 | **Provisório** (09/10/2026): filtro **`notaCorteMax`** de `GET /api/v1/cursos` **removido** enquanto não houver fonte de nota de corte; volta quando houver. Se o front mandar o parâmetro, ele é ignorado e a lista vem sem esse filtro. Os campos de resposta sem fonte (`notaCorteSisu`, `notaCorteSisuMin`, `mensalidade`, `notaMec`, `imagemUrl`, `logoUrl`) continuam no contrato (`imagemUrl` e `logoUrl` ganharam fonte na D-99 e D-101), vindo `null` (D-21), e estão descritos no Swagger com `@Schema` ("Ainda sem fonte de dados; vem null (P-11)"). | `CursoController.listar`, `FiltroOfertas`, DTOs de resposta |
| P-26 | Oferta (página) ausente no lote seguinte fica **inativa** (não é apagada). | `IngestaoService.importarLote` |
| P-14 | Sem gateway de pagamento, `PUT /api/v1/assinatura` age como pagamento confirmado: ativa a assinatura por 1 mês (ou troca o plano mantendo a data). Volta a depender do gateway quando ele existir (D-57). | `AssinaturaService.trocarPlano` |

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
| ~~P-09~~ | **Login e autenticação**: Supabase Auth ou login próprio (Spring Security)? | Resolvido: D-55 (login próprio), implementado na D-84 a D-87. |
| ~~P-10~~ | **Contradição no front**: a ADR-005 decidiu token em cookie HttpOnly, mas o `client.ts` lê o token do `localStorage`. | Resolvido: o `client.ts` usa `withCredentials` e não guarda token (D-84). |
| P-11 | **Fontes de dados ainda inexistentes**: mensalidade. Nota MEC resolvida pela D-49, notas de corte pela D-50/D-53, logos, capas e imagens dos cursos pela D-98 a D-101. Crédito das imagens: legenda "Foto: autor, licença" no front (D-100), a fazer quando o detalhe da universidade usar a API. | Mensalidade vem `null` (D-21). |
| P-12 | **Formas de ingresso e próximos prazos/editais** por universidade. | `FormaIngresso` não tem relação no DER. Próximo prazo resolvido pela D-105 (vem do calendário); formas de ingresso continuam sem fonte. |
| ~~P-13~~ | **"Na sua região"**: localização do usuário ou filtro de UF/cidade? | Resolvido: D-42 (UF do perfil). |
| P-14 | **Assinaturas e simulados**: planos, gateway de pagamento, conteúdo dos simulados. | Ainda em definição pelo time. |
| P-15 | **Deploy** da API e **agendamento** do scraper. | Onde rodam e com que frequência. |
| P-16 | **Ambientes**: um projeto Supabase ou dev e produção separados. | — |
| P-17 | **Histórico** de alterações de ofertas. | Sugestão para a v1: só o valor atual e a data da última coleta. |
| P-18 | **Branches e PRs**: quando sair da `main`, qual fluxo e padrão de commit (`TR-xxx`)? | Depende dos cards (D-11). |
| P-19 | **ADR-010 está como "proposto"** no front. | Validar formalmente com o time do front, agora que o backend adota o formato (D-15). |
| ~~P-33~~ | **Calendário acadêmico** (eventos de inscrição, prova, resultado, feiras; datas nacionais × vestibulares). | Resolvido: D-104 a D-107 (20/11/2026). Telas de admin e notificações ficam para depois (notificações com um colega). |

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
7. O detalhe da universidade ganha `enderecoSede` (D-58), exibido com o rótulo "Sede"; CEP só com dígitos (o front aplica a máscara). Em cursos/ofertas de campus fora da sede, mostrar só a cidade/UF da oferta, nunca o endereço da sede (nem como "Sede"); o endereço só aparece numa oferta quando a cidade/UF dela for igual à da sede. Campos `null` aparecem como "Sem informação".
8. O detalhe da universidade ganha `esfera` (D-64), `vagasPorAno` e `gratuita` (D-65), e `modalidades` passa a ser uma lista de objetos `{modalidade, disponivel, totalCursos, descricao}` (D-66). O mock do front usa `endereco`, e a API manda `enderecoSede` (D-58). Sem `distanciaKm` (D-51).