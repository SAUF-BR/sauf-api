CREATE TABLE indicador_ies (
    id             UUID PRIMARY KEY,
    instituicao_id UUID          NOT NULL REFERENCES instituicao (id),
    ano            INTEGER       NOT NULL,
    igc_continuo   NUMERIC(5, 3),
    igc_faixa      INTEGER CHECK (igc_faixa BETWEEN 1 AND 5),
    atualizado_em  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_indicador_ies_instituicao_ano UNIQUE (instituicao_id, ano)
);
