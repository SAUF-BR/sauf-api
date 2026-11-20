-- Notificações do usuário (TR-382): avisos de prazo, resultado, evento e boas-vindas.
-- A ação é opcional; quando existe, rotulo e para vêm juntos (par válido no front).
CREATE TABLE notificacao (
    id          UUID PRIMARY KEY,
    usuario_id  UUID         NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    titulo      VARCHAR(160) NOT NULL,
    descricao   VARCHAR(500) NOT NULL,
    acao_rotulo VARCHAR(80),
    acao_para   VARCHAR(500),
    lida        BOOLEAN      NOT NULL DEFAULT FALSE,
    criada_em   TIMESTAMP WITH TIME ZONE NOT NULL,
    lida_em     TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_notificacao_acao CHECK (
        (acao_rotulo IS NULL AND acao_para IS NULL)
        OR (acao_rotulo IS NOT NULL AND acao_para IS NOT NULL)
    )
);

CREATE INDEX idx_notificacao_usuario_criada ON notificacao (usuario_id, criada_em DESC);
CREATE INDEX idx_notificacao_usuario_lida ON notificacao (usuario_id, lida);
