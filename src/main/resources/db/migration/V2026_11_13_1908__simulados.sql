-- Simulados do ENEM (TR-409, D-80 a D-85). Enums em maiúsculas (D-30).
-- usuario_id sem chave estrangeira até o login existir (D-55).
-- tamanho é nulo no "refazer erros"; origem_id aponta o simulado de onde vieram as questões erradas.
-- acertos, erros e em_branco só são preenchidos na finalização.
CREATE TABLE simulado (
    id                   UUID PRIMARY KEY,
    usuario_id           UUID        NOT NULL,
    banca                VARCHAR(10) NOT NULL,
    tamanho              VARCHAR(10),
    cronometrado         BOOLEAN     NOT NULL,
    segundos_por_questao INTEGER,
    status               VARCHAR(20) NOT NULL,
    iniciado_em          TIMESTAMP WITH TIME ZONE NOT NULL,
    finalizado_em        TIMESTAMP WITH TIME ZONE,
    acertos              INTEGER,
    erros                INTEGER,
    em_branco            INTEGER,
    origem_id            UUID REFERENCES simulado (id)
);

CREATE INDEX idx_simulado_usuario ON simulado (usuario_id, iniciado_em DESC);

CREATE TABLE simulado_area (
    simulado_id UUID       NOT NULL REFERENCES simulado (id) ON DELETE CASCADE,
    area        VARCHAR(2) NOT NULL,
    PRIMARY KEY (simulado_id, area)
);

-- correta fica nula até a finalização. atualizada_em é nula enquanto o aluno não mexe na questão.
CREATE TABLE simulado_questao (
    simulado_id         UUID        NOT NULL REFERENCES simulado (id) ON DELETE CASCADE,
    ordem               INTEGER     NOT NULL,
    questao_id          UUID        NOT NULL REFERENCES questao (id) ON DELETE RESTRICT,
    situacao            VARCHAR(20) NOT NULL,
    alternativa_marcada VARCHAR(2),
    correta             BOOLEAN,
    tempo_segundos      INTEGER     NOT NULL DEFAULT 0,
    atualizada_em       TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (simulado_id, ordem),
    CONSTRAINT uk_simulado_questao UNIQUE (simulado_id, questao_id)
);

CREATE INDEX idx_simulado_questao_questao ON simulado_questao (questao_id);
