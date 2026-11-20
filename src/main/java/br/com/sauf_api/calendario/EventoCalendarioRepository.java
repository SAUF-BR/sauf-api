package br.com.sauf_api.calendario;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventoCalendarioRepository extends JpaRepository<EventoCalendario, UUID> {

    Optional<EventoCalendario> findByChave(String chave);

    /** Eventos com pelo menos um dia entre {@code de} e {@code ate} (um período conta em todos os dias). */
    @Query("""
            select e from EventoCalendario e left join fetch e.instituicao
            where e.inicio <= :ate and coalesce(e.fim, e.inicio) >= :de
            order by e.inicio, e.titulo""")
    List<EventoCalendario> noPeriodo(@Param("de") LocalDate de, @Param("ate") LocalDate ate);

    /** Eventos cujo prazo (fim do período ou o dia) ainda não passou, do mais próximo ao mais distante. */
    @Query("""
            select e from EventoCalendario e left join fetch e.instituicao
            where coalesce(e.fim, e.inicio) >= :hoje
            order by coalesce(e.fim, e.inicio), e.titulo""")
    List<EventoCalendario> comPrazoAPartirDe(@Param("hoje") LocalDate hoje, Pageable pagina);

    @Query("""
            select e from EventoCalendario e
            where e.instituicao.id = :instituicaoId and coalesce(e.fim, e.inicio) >= :hoje
            order by coalesce(e.fim, e.inicio), e.titulo""")
    List<EventoCalendario> daInstituicaoComPrazoAPartirDe(
            @Param("instituicaoId") UUID instituicaoId, @Param("hoje") LocalDate hoje, Pageable pagina);
}
