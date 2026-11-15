-- Login e cadastro próprios (D-84 a D-87).
-- Data em que a pessoa aceitou os termos de uso no cadastro (null para contas anteriores).
ALTER TABLE usuario ADD COLUMN termos_aceitos_em TIMESTAMP WITH TIME ZONE;
