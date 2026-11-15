-- Usuário e foto de perfil (área de Perfil, D-80 a D-82).
-- O e-mail é gravado em minúsculas pela API, por isso o UNIQUE simples basta.
CREATE TABLE usuario (
    id                UUID PRIMARY KEY,
    nome              VARCHAR(120) NOT NULL,
    email             VARCHAR(254) NOT NULL,
    senha_hash        VARCHAR(100),
    email_verificado  BOOLEAN      NOT NULL DEFAULT FALSE,
    nota_enem         NUMERIC(6, 2),
    senha_alterada_em TIMESTAMP WITH TIME ZONE,
    criado_em         TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_usuario_email UNIQUE (email)
);

-- Foto em tabela própria: a imagem não é carregada junto com o usuário (D-81).
CREATE TABLE usuario_foto (
    usuario_id    UUID PRIMARY KEY REFERENCES usuario (id) ON DELETE CASCADE,
    conteudo      BYTEA       NOT NULL,
    tipo          VARCHAR(30) NOT NULL,
    atualizada_em TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Usuário provisório (D-55): todas as requisições usam ele até o login existir.
INSERT INTO usuario (id, nome, email, email_verificado, criado_em)
VALUES ('f9def6d7-0717-4540-8ad0-ec03e301b2c7', 'Usuário de teste', 'teste@sauf.local', TRUE, CURRENT_TIMESTAMP);

-- Fecha a pendência da D-55: a assinatura passa a apontar para o usuário.
ALTER TABLE assinatura
    ADD CONSTRAINT fk_assinatura_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id);
