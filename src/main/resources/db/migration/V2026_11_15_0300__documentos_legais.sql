-- Documentos legais versionados e aceites (D-93). Baseado no rascunho V__termos_e_aceites.sql
-- do Carlos, sem a parte de administração (rascunho/publicação por tela): os textos vêm dos
-- arquivos .md da API (src/main/resources/documentos-legais), copiados para cá ao subir.

-- Cada versão publicada de Termos de Uso e Política de Privacidade. O texto e o hash ficam
-- guardados: é a prova do que a pessoa aceitou. A API nunca altera nem apaga uma versão que
-- já tenha aceites (DocumentosLegaisSincronizador).
CREATE TABLE documento_legal_versao (
    id                  UUID PRIMARY KEY,
    tipo                VARCHAR(40)  NOT NULL,
    versao              VARCHAR(20)  NOT NULL,
    titulo              VARCHAR(200) NOT NULL,
    conteudo_markdown   TEXT         NOT NULL,
    conteudo_sha256     CHAR(64)     NOT NULL,
    resumo_alteracoes   TEXT,
    exige_novo_aceite   BOOLEAN      NOT NULL,
    vigente_a_partir_de DATE         NOT NULL,
    publicada_em        TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_documento_tipo_versao UNIQUE (tipo, versao)
);

-- Um registro por aceite (só inserção). IP e navegador servem de prova (LGPD).
CREATE TABLE aceite_documento (
    id                  UUID PRIMARY KEY,
    usuario_id          UUID         NOT NULL REFERENCES usuario (id),
    documento_versao_id UUID         NOT NULL REFERENCES documento_legal_versao (id),
    conteudo_sha256     CHAR(64)     NOT NULL,
    aceito_em           TIMESTAMP WITH TIME ZONE NOT NULL,
    ip                  VARCHAR(45)  NOT NULL,
    user_agent          VARCHAR(500),
    canal               VARCHAR(10)  NOT NULL,
    CONSTRAINT uq_aceite_usuario_versao UNIQUE (usuario_id, documento_versao_id)
);

CREATE INDEX idx_aceite_usuario ON aceite_documento (usuario_id);

-- O aceite do texto fictício antigo não vale para estes documentos: quem já tem conta
-- aceita os documentos reais no próximo acesso.
ALTER TABLE usuario DROP COLUMN termos_aceitos_em;
