package br.com.sauf_api.simulado;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SimuladoQuestaoRepository extends JpaRepository<SimuladoQuestao, SimuladoQuestaoId> {

    /** Todas as questões que o usuário já teve em algum simulado. */
    @Query("select sq.questao.id from SimuladoQuestao sq where sq.simulado.usuarioId = :usuario")
    List<UUID> idsDoUsuario(UUID usuario);

    @Query("""
            select count(sq) from SimuladoQuestao sq
            where sq.simulado.usuarioId = :usuario and sq.situacao = :situacao
            """)
    long contar(UUID usuario, SituacaoQuestao situacao);

    @Query("""
            select sq from SimuladoQuestao sq join fetch sq.questao
            where sq.simulado.usuarioId = :usuario and sq.atualizadaEm >= :desde
            """)
    List<SimuladoQuestao> atividadeDesde(UUID usuario, Instant desde);
}
