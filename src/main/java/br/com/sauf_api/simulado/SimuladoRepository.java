package br.com.sauf_api.simulado;

import br.com.sauf_api.questao.Banca;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SimuladoRepository extends JpaRepository<Simulado, UUID> {

    Optional<Simulado> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    List<Simulado> findByUsuarioIdAndStatus(UUID usuarioId, StatusSimulado status);

    List<Simulado> findByUsuarioIdAndStatusAndFinalizadoEmGreaterThanEqual(
            UUID usuarioId, StatusSimulado status, Instant desde);

    @Query("""
            select s from Simulado s where s.usuarioId = :usuario and s.status in :status
            order by s.iniciadoEm desc
            """)
    Page<Simulado> recentes(UUID usuario, Collection<StatusSimulado> status, Pageable pageable);

    @Query("""
            select s from Simulado s where s.usuarioId = :usuario and s.status in :status
            order by s.iniciadoEm asc
            """)
    Page<Simulado> antigos(UUID usuario, Collection<StatusSimulado> status, Pageable pageable);

    // Sem nota (em andamento) vai sempre para o fim.
    @Query("""
            select s from Simulado s where s.usuarioId = :usuario and s.status in :status
            order by case when s.acertos is null then 1 else 0 end,
                     (cast(s.acertos as double) / (s.acertos + s.erros + s.emBranco)) desc, s.iniciadoEm desc
            """)
    Page<Simulado> maiorNota(UUID usuario, Collection<StatusSimulado> status, Pageable pageable);

    @Query("""
            select s from Simulado s where s.usuarioId = :usuario and s.status in :status
            order by case when s.acertos is null then 1 else 0 end,
                     (cast(s.acertos as double) / (s.acertos + s.erros + s.emBranco)) asc, s.iniciadoEm desc
            """)
    Page<Simulado> menorNota(UUID usuario, Collection<StatusSimulado> status, Pageable pageable);

    @Query("""
            select count(s) from Simulado s
            where s.usuarioId = :usuario and s.status = br.com.sauf_api.simulado.StatusSimulado.CONCLUIDO
              and s.banca in :bancas and s.finalizadoEm >= :desde and s.finalizadoEm < :ate
            """)
    long concluidosEntre(UUID usuario, Collection<Banca> bancas, Instant desde, Instant ate);

    @Query("""
            select s from Simulado s
            where s.usuarioId = :usuario and s.status = br.com.sauf_api.simulado.StatusSimulado.CONCLUIDO
              and s.banca in :bancas and s.finalizadoEm >= :desde and s.finalizadoEm < :ate
            order by s.finalizadoEm desc
            """)
    List<Simulado> ultimosConcluidos(
            UUID usuario, Collection<Banca> bancas, Instant desde, Instant ate, Pageable pageable);

    /** Alunos distintos e aproveitamento médio (em %) dos simulados concluídos de todos na janela. */
    @Query("""
            select new br.com.sauf_api.simulado.MediaPlataforma(
                count(distinct s.usuarioId),
                avg(cast(s.acertos as double) * 100 / (s.acertos + s.erros + s.emBranco)))
            from Simulado s
            where s.status = br.com.sauf_api.simulado.StatusSimulado.CONCLUIDO
              and s.banca in :bancas and s.finalizadoEm >= :desde and s.finalizadoEm < :ate
            """)
    MediaPlataforma mediaPlataforma(Collection<Banca> bancas, Instant desde, Instant ate);

    /**
     * Tempo de cada aluno nos simulados concluídos na janela, com cada questão limitada ao tempo
     * configurado (D-87; sem cronômetro vale o padrão). Desempate: mais simulados e depois quem
     * chegou ao total primeiro (o último simulado concluído mais cedo).
     */
    @Query(nativeQuery = true, value = """
            WITH tempos AS (
                SELECT s.usuario_id AS usuario_id,
                       SUM(LEAST(sq.tempo_segundos, COALESCE(s.segundos_por_questao, :padrao))) AS segundos,
                       COUNT(DISTINCT s.id) AS simulados,
                       MAX(s.finalizado_em) AS ultimo
                FROM simulado s
                JOIN simulado_questao sq ON sq.simulado_id = s.id
                WHERE s.status = 'CONCLUIDO' AND s.banca IN (:bancas)
                  AND s.finalizado_em >= :desde AND s.finalizado_em < :ate
                GROUP BY s.usuario_id
            )
            SELECT CAST(usuario_id AS VARCHAR(36)) AS usuarioId, segundos, simulados,
                   RANK() OVER (ORDER BY segundos DESC, simulados DESC, ultimo ASC) AS posicao
            FROM tempos
            WHERE segundos > 0
            ORDER BY posicao, usuario_id
            """)
    List<LinhaRanking> ranking(Collection<String> bancas, Instant desde, Instant ate, int padrao);
}
