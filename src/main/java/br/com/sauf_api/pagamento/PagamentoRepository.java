package br.com.sauf_api.pagamento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagamentoRepository extends JpaRepository<Pagamento, UUID> {

    Optional<Pagamento> findByStripeInvoiceId(String stripeInvoiceId);

    List<Pagamento> findByAssinaturaIdOrderByCriadoEmDesc(UUID assinaturaId);
}
