package br.com.sauf_api.simulado;

import br.com.sauf_api.questao.Banca;
import java.time.Instant;
import java.util.Collection;
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

    /** Resultado das questões dos simulados concluídos na janela, por área e habilidade. */
    @Query("""
            select new br.com.sauf_api.simulado.ContagemResultado(q.area, q.habilidade, sq.situacao, sq.correta, count(sq))
            from SimuladoQuestao sq join sq.questao q join sq.simulado s
            where s.usuarioId = :usuario and s.status = br.com.sauf_api.simulado.StatusSimulado.CONCLUIDO
              and s.banca in :bancas and s.finalizadoEm >= :desde and s.finalizadoEm < :ate
            group by q.area, q.habilidade, sq.situacao, sq.correta
            """)
    List<ContagemResultado> resultadosConcluidos(UUID usuario, Collection<Banca> bancas, Instant desde, Instant ate);

    /** Respostas dadas na janela, em simulados concluídos ou não. */
    @Query("""
            select count(sq) from SimuladoQuestao sq
            where sq.simulado.usuarioId = :usuario and sq.simulado.banca in :bancas
              and sq.situacao = br.com.sauf_api.simulado.SituacaoQuestao.RESPONDIDA
              and sq.atualizadaEm >= :desde and sq.atualizadaEm < :ate
            """)
    long respondidasEntre(UUID usuario, Collection<Banca> bancas, Instant desde, Instant ate);

    /** Quando o aluno mexeu em alguma questão (a mesma regra de "ativa" do cronômetro). */
    @Query("""
            select sq.atualizadaEm from SimuladoQuestao sq
            where sq.simulado.usuarioId = :usuario and sq.simulado.banca in :bancas
              and sq.atualizadaEm >= :desde
              and (sq.situacao <> br.com.sauf_api.simulado.SituacaoQuestao.PENDENTE or sq.tempoSegundos > 0)
            """)
    List<Instant> momentosDeEstudo(UUID usuario, Collection<Banca> bancas, Instant desde);
}
