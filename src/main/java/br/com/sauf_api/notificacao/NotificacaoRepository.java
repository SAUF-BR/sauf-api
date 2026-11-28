package br.com.sauf_api.notificacao;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificacaoRepository extends JpaRepository<Notificacao, UUID> {

    List<Notificacao> findByUsuarioIdOrderByCriadaEmDesc(UUID usuarioId);

    long countByUsuarioIdAndLidaFalse(UUID usuarioId);

    // clearAutomatically: apaga o cache de primeiro nível para que uma listagem
    // feita logo depois (mesma transação) enxergue as notificações como lidas.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Notificacao n
               SET n.lida = true, n.lidaEm = :momento
             WHERE n.usuarioId = :usuarioId
               AND n.lida = false
            """)
    int marcarTodasComoLidas(@Param("usuarioId") UUID usuarioId, @Param("momento") Instant momento);
}
