-- Resposta fora do tempo (TR-409, D-86): a questão com tempo esgotado continua aberta e
-- fora_do_tempo marca a resposta dada depois do limite. TEMPO_ESGOTADO deixa de existir.
ALTER TABLE simulado_questao ADD COLUMN fora_do_tempo BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE simulado_questao
SET situacao = CASE
        WHEN (SELECT s.status FROM simulado s WHERE s.id = simulado_questao.simulado_id) = 'CONCLUIDO'
            THEN 'EM_BRANCO'
        ELSE 'PENDENTE'
    END
WHERE situacao = 'TEMPO_ESGOTADO';
