-- Planos passam a ser Free, Basic e Plus (D-98). O Premium deixa de existir e o Plus
-- ganhou tudo o que ele tinha, então as assinaturas Premium viram Plus.
-- O Free não tem linhas: é o plano de quem não assina.
UPDATE assinatura SET plano = 'PLUS' WHERE plano = 'PREMIUM';
