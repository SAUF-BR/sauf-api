package br.com.sauf_api.notificacao;

import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificacaoEventoGeradaRepository
        extends JpaRepository<NotificacaoEventoGerada, NotificacaoEventoGerada.Id> {

    /** IDs de usuários que já receberam aviso daquele evento × janela — para filtrar em memória. */
    @Query("""
            SELECT g.id.usuarioId FROM NotificacaoEventoGerada g
             WHERE g.id.eventoId = :eventoId
               AND g.id.janelaDias = :janelaDias
            """)
    Set<UUID> usuariosJaAvisados(@Param("eventoId") UUID eventoId, @Param("janelaDias") int janelaDias);
}
