package br.com.sauf_api.auth;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TokenEmailRepository extends JpaRepository<TokenEmail, UUID> {

    /**
     * O código ainda não usado mais recente da pessoa. Travado para escrita: duas tentativas
     * ao mesmo tempo não passam do limite de 5 nem usam o código duas vezes.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TokenEmail> findFirstByUsuarioIdAndTipoAndUsadoEmIsNullOrderByCriadoEmDesc(
            UUID usuarioId, TipoTokenEmail tipo);

    // Quando foi pedido o último código deste tipo (evita mandar vários e-mails seguidos)
    @Query("select max(t.criadoEm) from TokenEmail t where t.usuarioId = :usuarioId and t.tipo = :tipo")
    Optional<Instant> ultimoCriadoEm(UUID usuarioId, TipoTokenEmail tipo);

    // Um código novo invalida os anteriores do mesmo tipo
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update TokenEmail t set t.usadoEm = :agora "
            + "where t.usuarioId = :usuarioId and t.tipo = :tipo and t.usadoEm is null")
    void invalidarPendentes(UUID usuarioId, TipoTokenEmail tipo, Instant agora);
}
