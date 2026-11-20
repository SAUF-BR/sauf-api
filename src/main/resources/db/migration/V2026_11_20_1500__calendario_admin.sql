-- Calendário de vestibulares (TR-343, D-104 a D-106) e papel do usuário para as rotas de
-- administração (D-107).

-- Quem está na SAUF_ADMIN_EMAILS vira ADMIN ao usar /api/v1/admin/** (D-107).
ALTER TABLE usuario ADD COLUMN papel VARCHAR(20) NOT NULL DEFAULT 'ESTUDANTE';
ALTER TABLE usuario ADD CONSTRAINT ck_usuario_papel CHECK (papel IN ('ESTUDANTE', 'ADMIN'));

-- Um evento por linha: um dia (fim nulo) ou um período (inicio a fim). A chave é o nome
-- estável que o scraper e as futuras telas de admin usam para reenviar sem duplicar.
-- instituicao_id nulo = evento nacional (ENEM, ProUni, Sisu).
CREATE TABLE evento_calendario (
    id             UUID PRIMARY KEY,
    chave          VARCHAR(100) NOT NULL,
    titulo         VARCHAR(200) NOT NULL,
    tipo           VARCHAR(20)  NOT NULL,
    inicio         DATE         NOT NULL,
    fim            DATE,
    instituicao_id UUID REFERENCES instituicao (id),
    descricao      VARCHAR(1000),
    fonte_url      VARCHAR(500) NOT NULL,
    ano_referencia INTEGER      NOT NULL,
    atualizado_em  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_evento_calendario_chave UNIQUE (chave),
    CONSTRAINT ck_evento_calendario_tipo CHECK (tipo IN ('INSCRICAO', 'PROVA', 'RESULTADO', 'FEIRA', 'OUTRO')),
    CONSTRAINT ck_evento_calendario_periodo CHECK (fim IS NULL OR fim >= inicio)
);

CREATE INDEX ix_evento_calendario_inicio ON evento_calendario (inicio);
CREATE INDEX ix_evento_calendario_instituicao ON evento_calendario (instituicao_id);
