-- Memória do gerador de notificações do calendário (TR-383): garante que um aviso
-- de um evento × janela × usuário é criado só uma vez, mesmo com o job rodando
-- várias vezes por dia.
CREATE TABLE notificacao_evento_gerada (
    evento_id   UUID    NOT NULL REFERENCES evento_calendario (id) ON DELETE CASCADE,
    usuario_id  UUID    NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    janela_dias INTEGER NOT NULL,
    gerada_em   TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (evento_id, usuario_id, janela_dias)
);

CREATE INDEX idx_notif_evento_gerada_usuario ON notificacao_evento_gerada (usuario_id);
