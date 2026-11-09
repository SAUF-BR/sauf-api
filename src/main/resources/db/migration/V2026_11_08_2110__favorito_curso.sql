-- Cursos favoritos do estudante: o favorito aponta para a oferta, o curso numa universidade (D-38).
-- usuario_id ainda sem chave estrangeira: a tabela de usuário chega com o login (D-55).
CREATE TABLE favorito_curso (
    id         UUID PRIMARY KEY,
    usuario_id UUID NOT NULL,
    oferta_id  UUID NOT NULL REFERENCES oferta_curso (id),
    criado_em  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_favorito_curso UNIQUE (usuario_id, oferta_id)
);
