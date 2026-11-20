-- Dados cadastrados à mão no scraper (sauf-scraper/dados, D-98 a D-103, ADR-BE-017).
-- O Censo e os spiders não mexem nessas colunas nem nessas tabelas.

ALTER TABLE instituicao ADD COLUMN ano_fundacao INTEGER;
-- Parágrafos separados por linha em branco, como o sobre da oferta (D-37)
ALTER TABLE instituicao ADD COLUMN sobre TEXT;
ALTER TABLE instituicao ADD COLUMN dados_manuais_em TIMESTAMP WITH TIME ZONE;

-- Logo e capa da universidade, com a licença de cada arquivo (Wikimedia Commons, D-100)
CREATE TABLE instituicao_imagem (
    instituicao_id UUID         NOT NULL REFERENCES instituicao (id) ON DELETE CASCADE,
    tipo           VARCHAR(10)  NOT NULL,
    conteudo       BYTEA        NOT NULL,
    tipo_mime      VARCHAR(30)  NOT NULL,
    hash           VARCHAR(64)  NOT NULL,
    autor          VARCHAR(300) NOT NULL,
    licenca        VARCHAR(100) NOT NULL,
    licenca_url    VARCHAR(500),
    fonte_url      VARCHAR(500) NOT NULL,
    atualizada_em  TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (instituicao_id, tipo),
    CONSTRAINT ck_instituicao_imagem_tipo CHECK (tipo IN ('LOGO', 'CAPA'))
);

-- Uma imagem por área Cine; vira a imagem dos cursos da área (D-101)
CREATE TABLE area_imagem (
    area_id       UUID         PRIMARY KEY REFERENCES area (id) ON DELETE CASCADE,
    conteudo      BYTEA        NOT NULL,
    tipo_mime     VARCHAR(30)  NOT NULL,
    hash          VARCHAR(64)  NOT NULL,
    autor         VARCHAR(300) NOT NULL,
    licenca       VARCHAR(100) NOT NULL,
    licenca_url   VARCHAR(500),
    fonte_url     VARCHAR(500) NOT NULL,
    atualizada_em TIMESTAMP WITH TIME ZONE NOT NULL
);
