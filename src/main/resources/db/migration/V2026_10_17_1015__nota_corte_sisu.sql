-- Nota de corte do Sisu, ampla concorrência, por (curso e-MEC, turno, ano) (D-50).
-- nota NULL = o Sisu não teve nota de corte nesse par (NU_NOTACORTE 0).
CREATE TABLE nota_corte_sisu (
    id                UUID PRIMARY KEY,
    instituicao_id    UUID         NOT NULL REFERENCES instituicao (id),
    codigo_emec_curso INTEGER      NOT NULL,
    turno             VARCHAR(20)  NOT NULL,
    ano               INTEGER      NOT NULL,
    vagas             INTEGER,
    inscricoes        INTEGER,
    nota              NUMERIC(6, 2),
    atualizado_em     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_nota_corte_sisu_curso_turno_ano UNIQUE (codigo_emec_curso, turno, ano)
);

CREATE INDEX idx_nota_corte_sisu_instituicao ON nota_corte_sisu (instituicao_id);
