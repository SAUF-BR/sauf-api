package br.com.sauf_api.simulado;

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
}
