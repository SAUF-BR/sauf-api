-- Assinaturas dos estudantes (D-54 a D-57).
-- usuario_id ainda sem chave estrangeira: a tabela de usuário chega com o login (D-55).
CREATE TABLE assinatura (
    id         UUID PRIMARY KEY,
    usuario_id UUID        NOT NULL,
    plano      VARCHAR(20) NOT NULL,
    status     VARCHAR(30) NOT NULL,
    renova_em  DATE        NOT NULL,
    criada_em  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_assinatura_usuario ON assinatura (usuario_id, criada_em);
