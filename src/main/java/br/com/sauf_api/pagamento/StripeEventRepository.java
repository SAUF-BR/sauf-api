package br.com.sauf_api.pagamento;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StripeEventRepository extends JpaRepository<StripeEvent, String> {}
