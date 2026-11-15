-- "Esqueci minha senha" por código de 6 dígitos em vez de link (D-90, revisada em 14/11/2026).
-- O código é curto: dois usuários podem receber o mesmo, então o hash deixa de ser único
-- (a busca passa a ser pelo usuário) e passa a ser BCrypt. As tentativas erradas são contadas
-- (no máximo 5 por código).
ALTER TABLE token_email DROP CONSTRAINT uq_token_email_hash;
ALTER TABLE token_email ADD COLUMN tentativas INT NOT NULL DEFAULT 0;

-- Links de redefinição antigos deixam de valer
UPDATE token_email SET usado_em = CURRENT_TIMESTAMP WHERE usado_em IS NULL;
