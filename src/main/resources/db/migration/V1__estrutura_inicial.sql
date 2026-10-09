-- Estrutura inicial do banco do SAUF-BR (decisões em docs/DECISOES.md).

CREATE TABLE area (
    id          UUID PRIMARY KEY,
    codigo_cine VARCHAR(2)   NOT NULL UNIQUE,
    nome        VARCHAR(120) NOT NULL
);

CREATE TABLE instituicao (
    id                       UUID PRIMARY KEY,
    codigo_emec              INTEGER      NOT NULL UNIQUE,
    nome                     VARCHAR(200) NOT NULL,
    sigla                    VARCHAR(20),
    tipo                     VARCHAR(20)  NOT NULL,
    categoria_administrativa VARCHAR(40),
    organizacao_academica    VARCHAR(40),
    uf                       VARCHAR(2)   NOT NULL,
    cidade                   VARCHAR(150) NOT NULL,
    site_url                 VARCHAR(500),
    logo_url                 VARCHAR(500),
    nota_mec                 INTEGER,
    ativa                    BOOLEAN      NOT NULL DEFAULT TRUE,
    atualizada_em            TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Curso genérico do catálogo (D-17, D-31, D-36). Sem área = pendente de revisão (P-05).
CREATE TABLE curso (
    id                 UUID PRIMARY KEY,
    nome               VARCHAR(200) NOT NULL,
    area_id            UUID REFERENCES area (id),
    codigo_cine_rotulo VARCHAR(10) UNIQUE,
    pendente_revisao   BOOLEAN      NOT NULL DEFAULT FALSE,
    imagem_url         VARCHAR(500),
    CONSTRAINT uk_curso_nome_area UNIQUE (nome, area_id)
);

-- Cursos do Censo da Educação Superior: só complementam as ofertas do site (D-33).
CREATE TABLE censo_curso (
    codigo_emec    INTEGER PRIMARY KEY,
    instituicao_id UUID         NOT NULL REFERENCES instituicao (id),
    curso_id       UUID         NOT NULL REFERENCES curso (id),
    nome           VARCHAR(200) NOT NULL,
    grau           VARCHAR(30),
    modalidade     VARCHAR(20),
    cidade         VARCHAR(150),
    uf             VARCHAR(2),
    vagas          INTEGER,
    gratuito       BOOLEAN,
    ano_censo      INTEGER      NOT NULL
);

-- Uma página de curso no site da instituição (D-34).
CREATE TABLE oferta_curso (
    id                UUID PRIMARY KEY,
    curso_id          UUID         NOT NULL REFERENCES curso (id),
    instituicao_id    UUID         NOT NULL REFERENCES instituicao (id),
    chave             VARCHAR(200) NOT NULL,
    nome_no_site      VARCHAR(200) NOT NULL,
    modalidade        VARCHAR(20),
    turno             VARCHAR(200),
    sobre             TEXT,
    duracao_texto     VARCHAR(200),
    duracao_semestres INTEGER,
    uf                VARCHAR(2),
    cidade            VARCHAR(150),
    vagas             INTEGER,
    gratuito          BOOLEAN,
    url_origem        VARCHAR(500),
    nota_corte_sisu   NUMERIC(6, 2),
    mensalidade       NUMERIC(10, 2),
    ativa             BOOLEAN      NOT NULL DEFAULT TRUE,
    coletada_em       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_oferta_chave UNIQUE (instituicao_id, chave)
);

-- Graus que a página oferece, cada um com o turno que a página dá (D-34).
CREATE TABLE oferta_opcao (
    oferta_id UUID        NOT NULL REFERENCES oferta_curso (id) ON DELETE CASCADE,
    grau      VARCHAR(30) NOT NULL,
    turno     VARCHAR(200),
    PRIMARY KEY (oferta_id, grau)
);

-- Códigos e-MEC da página, uso interno para ligar ao Censo (D-35).
CREATE TABLE oferta_codigo_emec (
    oferta_id   UUID    NOT NULL REFERENCES oferta_curso (id) ON DELETE CASCADE,
    codigo_emec INTEGER NOT NULL,
    PRIMARY KEY (oferta_id, codigo_emec)
);

CREATE INDEX idx_instituicao_uf_cidade ON instituicao (uf, cidade);
CREATE INDEX idx_censo_curso_instituicao ON censo_curso (instituicao_id);
CREATE INDEX idx_oferta_curso ON oferta_curso (curso_id);
CREATE INDEX idx_oferta_instituicao ON oferta_curso (instituicao_id);

-- Áreas gerais do Censo, classificação Cine/Unesco (D-18).
INSERT INTO area (id, codigo_cine, nome) VALUES
    ('62bb38d3-7af3-43d8-ba8c-9183527513f2', '00', 'Programas básicos'),
    ('e5cf6ac9-1054-4bd2-ad67-24cda139dd8f', '01', 'Educação'),
    ('a2ac743d-79a9-4619-831d-5c008acdb1dc', '02', 'Artes e humanidades'),
    ('a0453a72-b2d0-4f39-8902-36a16138e3b7', '03', 'Ciências sociais, comunicação e informação'),
    ('8ebcfa22-b84e-4fb0-a8e0-e56e82b0289b', '04', 'Negócios, administração e direito'),
    ('1c23e88c-553a-44f0-8ddd-b61243d92e25', '05', 'Ciências naturais, matemática e estatística'),
    ('8a1eeb3d-2a5b-4f4b-a386-161f039182d6', '06', 'Computação e Tecnologias da Informação e Comunicação (TIC)'),
    ('685073bd-d04e-40fb-b783-a51edd6a2cad', '07', 'Engenharia, produção e construção'),
    ('91ce73c8-ee67-4f2f-8b54-442958bf99ba', '08', 'Agricultura, silvicultura, pesca e veterinária'),
    ('5e185e09-298a-47f5-818d-6a4e3998299d', '09', 'Saúde e bem-estar'),
    ('def7ebb3-b738-4158-bcea-7e547ea4d25f', '10', 'Serviços');
