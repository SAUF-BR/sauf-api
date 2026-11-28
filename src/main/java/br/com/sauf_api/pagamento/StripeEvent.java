package br.com.sauf_api.pagamento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Evento de webhook já recebido. O id é o do Stripe (evt_...): se o mesmo evento chegar
 * de novo, a chave primária impede o reprocessamento (idempotência, ADR-BE-016).
 */
@Entity
@Table(name = "stripe_event")
public class StripeEvent {

    @Id
    private String id;

    @Column(nullable = false, length = 100)
    private String tipo;

    @Column(name = "recebido_em", nullable = false)
    private Instant recebidoEm;

    @Column(name = "processado_em")
    private Instant processadoEm;

    protected StripeEvent() {}

    public StripeEvent(String id, String tipo) {
        this.id = id;
        this.tipo = tipo;
        this.recebidoEm = Instant.now();
    }

    public void marcarProcessado() {
        this.processadoEm = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public Instant getRecebidoEm() {
        return recebidoEm;
    }

    public Instant getProcessadoEm() {
        return processadoEm;
    }
}
