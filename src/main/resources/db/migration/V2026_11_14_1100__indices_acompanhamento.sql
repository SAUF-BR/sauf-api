-- Consultas do acompanhamento e do ranking (TR-409, D-87 a D-92): janelas por data de
-- finalização do simulado e por data da resposta; o ranking soma todos os alunos da janela.
CREATE INDEX idx_simulado_usuario_finalizado ON simulado (usuario_id, finalizado_em);
CREATE INDEX idx_simulado_finalizado ON simulado (status, finalizado_em);
CREATE INDEX idx_simulado_questao_atualizada ON simulado_questao (simulado_id, atualizada_em);
