-- Usuário de homologação (D-83): conta fixa, com assinatura Premium ativa até 31/12/2099,
-- para testar as telas de assinante sem os dados vencerem nem mudarem.
--
-- Rodar de novo a qualquer momento devolve a conta ao estado original (nome, nota, foto
-- e assinatura), mesmo depois de editar o perfil ou cancelar a assinatura pela tela.
-- NÃO é migration: roda só onde for chamado (banco local; no Supabase só se o time decidir).
--
-- Banco local:
--   docker compose exec -T postgres psql -U sauf -d sauf < docs/homologacao/usuario-homologacao.sql
--
-- Para entrar (tela de login ou POST /api/v1/auth/login, D-84):
--   e-mail: homologacao@sauf.local
--   senha:  Homolog@2026   (só de homologação; gravada como hash BCrypt, custo 10)

BEGIN;

INSERT INTO usuario (id, nome, email, senha_hash, email_verificado, nota_enem, senha_alterada_em, criado_em)
VALUES ('0b5e7c1a-4f2d-4c8e-9a6b-3d1f0e2a7c55', 'Usuária Homologação', 'homologacao@sauf.local',
        '$2a$10$uKWSdcBpmy79fSecqbalQeZvssle1A065MGyTXJGdfav53Wb18yue',
        TRUE, 720.50, TIMESTAMP WITH TIME ZONE '2026-03-10 12:00:00+00',
        TIMESTAMP WITH TIME ZONE '2026-03-10 12:00:00+00')
ON CONFLICT (id) DO UPDATE SET
    nome              = EXCLUDED.nome,
    email             = EXCLUDED.email,
    email_verificado  = EXCLUDED.email_verificado,
    nota_enem         = EXCLUDED.nota_enem,
    senha_hash        = EXCLUDED.senha_hash,
    senha_alterada_em = EXCLUDED.senha_alterada_em,
    criado_em         = EXCLUDED.criado_em;

-- Começa sem foto (a foto pode ser testada e é apagada ao rodar de novo)
DELETE FROM usuario_foto WHERE usuario_id = '0b5e7c1a-4f2d-4c8e-9a6b-3d1f0e2a7c55';

-- Uma assinatura só: Premium, ativa, renovando em 31/12/2099 (nunca vence, D-57)
DELETE FROM assinatura WHERE usuario_id = '0b5e7c1a-4f2d-4c8e-9a6b-3d1f0e2a7c55';
INSERT INTO assinatura (id, usuario_id, plano, status, renova_em, criada_em)
VALUES ('5c3d9e2b-8a1f-4b7c-b6d4-1e9f2a0c3b77', '0b5e7c1a-4f2d-4c8e-9a6b-3d1f0e2a7c55',
        'PREMIUM', 'ATIVA', DATE '2099-12-31', TIMESTAMP WITH TIME ZONE '2026-03-10 12:00:00+00');

COMMIT;
