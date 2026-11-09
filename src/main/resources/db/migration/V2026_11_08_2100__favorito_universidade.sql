-- Universidades favoritas do estudante (D-38).
-- usuario_id ainda sem chave estrangeira: a tabela de usuário chega com o login (D-55).
CREATE TABLE favorito_universidade (
       id             UUID PRIMARY KEY,
       usuario_id     UUID NOT NULL,
       instituicao_id UUID NOT NULL REFERENCES instituicao (id),
       criado_em      TIMESTAMP WITH TIME ZONE NOT NULL,
       CONSTRAINT uk_favorito_universidade UNIQUE (usuario_id, instituicao_id)
);