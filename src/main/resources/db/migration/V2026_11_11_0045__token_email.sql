-- Links enviados por e-mail (D-89, D-90): verificação do e-mail e redefinição da senha.
-- Guarda só o hash SHA-256 do token (quem lê o banco não consegue usar o link).
-- Cada link vale uma vez (usado_em) e até expira_em.
CREATE TABLE token_email (
    id         UUID PRIMARY KEY,
    usuario_id UUID        NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    tipo       VARCHAR(30) NOT NULL,
    hash       VARCHAR(64) NOT NULL,
    criado_em  TIMESTAMP WITH TIME ZONE NOT NULL,
    expira_em  TIMESTAMP WITH TIME ZONE NOT NULL,
    usado_em   TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_token_email_hash UNIQUE (hash)
);

CREATE INDEX idx_token_email_usuario ON token_email (usuario_id, tipo, criado_em);
