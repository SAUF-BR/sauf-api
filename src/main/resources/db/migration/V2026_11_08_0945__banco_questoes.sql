-- Banco de questões dos simulados (ADR-SC-010, D-68). Enums em maiúsculas (D-30).
-- Uma prova = um caderno de um dia (ENEM 2024 D1 LARANJA).
CREATE TABLE prova (
    id            UUID PRIMARY KEY,
    banca         VARCHAR(10)  NOT NULL,
    ano           INTEGER      NOT NULL,
    edicao        VARCHAR(20)  NOT NULL,
    caderno       VARCHAR(30)  NOT NULL,
    url_origem    TEXT,
    credito       TEXT         NOT NULL,
    atualizada_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_prova_banca_ano_edicao_caderno UNIQUE (banca, ano, edicao, caderno)
);

-- Texto de apoio em Markdown. Questões da mesma prova com o mesmo texto apontam para a
-- mesma linha (hash SHA-256 do conteúdo).
CREATE TABLE texto_base (
    id       UUID PRIMARY KEY,
    prova_id UUID        NOT NULL REFERENCES prova (id) ON DELETE CASCADE,
    conteudo TEXT        NOT NULL,
    hash     VARCHAR(64) NOT NULL,
    CONSTRAINT uk_texto_base_prova_hash UNIQUE (prova_id, hash)
);

-- lingua: '' quando a questão não é de língua estrangeira. Com NULL, o UNIQUE deixaria
-- repetir (prova, numero) no Postgres e no H2; o '' vira null no Java (LinguaConverter).
CREATE TABLE questao (
    id               UUID PRIMARY KEY,
    prova_id         UUID          NOT NULL REFERENCES prova (id) ON DELETE CASCADE,
    texto_base_id    UUID          REFERENCES texto_base (id),
    numero           INTEGER       NOT NULL,
    lingua           VARCHAR(10)   NOT NULL DEFAULT '',
    tipo             VARCHAR(20)   NOT NULL,
    area             VARCHAR(2)    NOT NULL,
    materia          VARCHAR(50),
    assunto          VARCHAR(100),
    habilidade       INTEGER,
    comando          TEXT          NOT NULL,
    gabarito_soma    INTEGER,
    dificuldade      NUMERIC(8, 5),
    anulada          BOOLEAN       NOT NULL,
    status           VARCHAR(20)   NOT NULL,
    motivo_pendencia VARCHAR(500),
    codigo_origem    INTEGER,
    credito          TEXT          NOT NULL,
    atualizada_em    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_questao_prova_numero_lingua UNIQUE (prova_id, numero, lingua)
);

CREATE INDEX idx_questao_texto_base ON questao (texto_base_id);
CREATE INDEX idx_questao_sorteio ON questao (status, area);

-- rotulo: A a E (múltipla escolha) ou 01 a 16 (somatória, com valor).
CREATE TABLE alternativa (
    questao_id UUID        NOT NULL REFERENCES questao (id) ON DELETE CASCADE,
    rotulo     VARCHAR(2)  NOT NULL,
    valor      INTEGER,
    texto      TEXT        NOT NULL,
    correta    BOOLEAN     NOT NULL,
    PRIMARY KEY (questao_id, rotulo)
);
