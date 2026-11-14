-- Alunos fictícios para ver o ranking e a média da plataforma no banco LOCAL (TR-409, D-87 a D-92).
-- NUNCA rodar no Supabase: só no sauf_completo do Docker (porta 5433). Não é migration.
-- Os perfis (nome inventado, cidade, UF, opt-in, menor de idade) ficam em
-- src/main/resources/mock/perfis-publicos.json, com os mesmos UUIDs (5eed0000-...).
-- Pode rodar de novo: apaga e recria só os simulados desses alunos.
--
-- Uso: psql -h localhost -p 5433 -U <usuário> -d sauf_completo -f scripts/seed-ranking-local.sql

DO $$
DECLARE
    aluno     RECORD;
    sim       INTEGER;
    ordem     INTEGER;
    id_sim    UUID;
    fim       TIMESTAMPTZ;
    v_acertos   INTEGER;
    v_erros     INTEGER;
    v_em_branco INTEGER;
    tempo     INTEGER;
    sorteio   DOUBLE PRECISION;
    questoes  UUID[];
BEGIN
    IF current_database() <> 'sauf_completo' THEN
        RAISE EXCEPTION 'Seed só para o banco local sauf_completo (atual: %)', current_database();
    END IF;

    PERFORM setseed(0.409);
    DELETE FROM simulado WHERE usuario_id::text LIKE '5eed0000-%';

    SELECT array_agg(id ORDER BY id) INTO questoes
    FROM questao WHERE status = 'DISPONIVEL' AND NOT anulada;
    IF coalesce(array_length(questoes, 1), 0) < 10 THEN
        RAISE EXCEPTION 'Banco sem questões suficientes para o seed';
    END IF;

    FOR aluno IN SELECT * FROM (VALUES
    ('5eed0000-0000-4000-8000-000000000001'::uuid, 14, 140),
    ('5eed0000-0000-4000-8000-000000000002'::uuid, 13, 145),
    ('5eed0000-0000-4000-8000-000000000003'::uuid, 12, 138),
    ('5eed0000-0000-4000-8000-000000000004'::uuid, 12, 120),
    ('5eed0000-0000-4000-8000-000000000005'::uuid, 11, 130),
    ('5eed0000-0000-4000-8000-000000000006'::uuid, 11, 118),
    ('5eed0000-0000-4000-8000-000000000007'::uuid, 10, 135),
    ('5eed0000-0000-4000-8000-000000000008'::uuid, 10, 110),
    ('5eed0000-0000-4000-8000-000000000009'::uuid, 9, 140),
    ('5eed0000-0000-4000-8000-000000000010'::uuid, 9, 125),
    ('5eed0000-0000-4000-8000-000000000011'::uuid, 9, 100),
    ('5eed0000-0000-4000-8000-000000000012'::uuid, 8, 132),
    ('5eed0000-0000-4000-8000-000000000013'::uuid, 8, 120),
    ('5eed0000-0000-4000-8000-000000000014'::uuid, 8, 105),
    ('5eed0000-0000-4000-8000-000000000015'::uuid, 7, 140),
    ('5eed0000-0000-4000-8000-000000000016'::uuid, 7, 118),
    ('5eed0000-0000-4000-8000-000000000017'::uuid, 7, 110),
    ('5eed0000-0000-4000-8000-000000000018'::uuid, 6, 130),
    ('5eed0000-0000-4000-8000-000000000019'::uuid, 6, 115),
    ('5eed0000-0000-4000-8000-000000000020'::uuid, 6, 100),
    ('5eed0000-0000-4000-8000-000000000021'::uuid, 5, 125),
    ('5eed0000-0000-4000-8000-000000000022'::uuid, 5, 110),
    ('5eed0000-0000-4000-8000-000000000023'::uuid, 4, 135),
    ('5eed0000-0000-4000-8000-000000000024'::uuid, 4, 120),
    ('5eed0000-0000-4000-8000-000000000025'::uuid, 4, 95),
    ('5eed0000-0000-4000-8000-000000000026'::uuid, 3, 130),
    ('5eed0000-0000-4000-8000-000000000027'::uuid, 3, 100),
    ('5eed0000-0000-4000-8000-000000000028'::uuid, 2, 125),
    ('5eed0000-0000-4000-8000-000000000029'::uuid, 2, 90),
    ('5eed0000-0000-4000-8000-000000000030'::uuid, 1, 110)
    ) AS t(usuario_id, simulados, segundos) LOOP
        -- Simulados no mês atual e alguns no anterior, para a colocação anterior e a variação
        FOR sim IN 1..(aluno.simulados + aluno.simulados / 2) LOOP
            id_sim := gen_random_uuid();
            fim := date_trunc('day', now()) - make_interval(days => (random() * 45)::int)
                   + make_interval(hours => 8 + (random() * 14)::int);
            IF fim > now() THEN
                fim := now() - interval '1 hour';
            END IF;
            v_acertos := 0; v_erros := 0; v_em_branco := 0;

            INSERT INTO simulado (id, usuario_id, banca, tamanho, cronometrado, segundos_por_questao, status,
                                  iniciado_em, finalizado_em)
            VALUES (id_sim, aluno.usuario_id, 'ENEM', 'RAPIDA', TRUE, 150, 'CONCLUIDO',
                    fim - interval '30 minutes', fim);

            FOR ordem IN 1..10 LOOP
                sorteio := random();
                -- Às vezes passa do limite de 150 s: o ranking só conta até o limite (D-87)
                tempo := greatest(20, (aluno.segundos * (0.6 + random() * 0.8))::int);
                INSERT INTO simulado_questao (simulado_id, ordem, questao_id, situacao, alternativa_marcada,
                                              correta, tempo_segundos, atualizada_em, fora_do_tempo)
                VALUES (id_sim, ordem,
                        questoes[1 + ((abs(hashtext(id_sim::text)) + ordem * 7919) % array_length(questoes, 1))],
                        CASE WHEN sorteio < 0.92 THEN 'RESPONDIDA' ELSE 'EM_BRANCO' END,
                        CASE WHEN sorteio < 0.92 THEN 'A' END,
                        sorteio < 0.62, tempo, fim - make_interval(mins => 30 - ordem * 3), FALSE);
                IF sorteio < 0.62 THEN v_acertos := v_acertos + 1;
                ELSIF sorteio < 0.92 THEN v_erros := v_erros + 1;
                ELSE v_em_branco := v_em_branco + 1;
                END IF;
            END LOOP;

            UPDATE simulado s SET acertos = v_acertos, erros = v_erros, em_branco = v_em_branco WHERE s.id = id_sim;
        END LOOP;
    END LOOP;
END $$;
