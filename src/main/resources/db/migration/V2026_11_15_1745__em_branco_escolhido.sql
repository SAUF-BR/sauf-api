-- Em branco é só a questão em que o aluno clicou em "Deixar em branco" (TR-409, D-96).
-- Até aqui a finalização convertia as pendentes em EM_BRANCO. Nos simulados concluídos,
-- volta para PENDENTE a questão que o aluno nunca tocou (atualizada_em nula); as tocadas sem
-- resposta não dá para separar e continuam EM_BRANCO.
UPDATE simulado_questao
SET situacao = 'PENDENTE'
WHERE situacao = 'EM_BRANCO'
  AND atualizada_em IS NULL
  AND simulado_id IN (SELECT id FROM simulado WHERE status = 'CONCLUIDO');

-- fora_do_tempo (D-86) deixa de ser usado: o tempo por questão é só referência (D-95).
-- A coluna fica, com o valor padrão, para não apagar o histórico.
COMMENT ON COLUMN simulado_questao.fora_do_tempo IS 'Sem uso desde a D-95 (tempo por questão só como referência)';
