package br.com.sauf_api.pagamento;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StripeEventRepository extends JpaRepository<StripeEvent, String> {

    /** Devolve 0 se o evento já existe. */
    @Modifying
    @Query(value = """
            INSERT INTO stripe_event (id, tipo, recebido_em)
            VALUES (:id, :tipo, :recebidoEm)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int inserirSeNovo(@Param("id") String id, @Param("tipo") String tipo, @Param("recebidoEm") Instant recebidoEm);
}
