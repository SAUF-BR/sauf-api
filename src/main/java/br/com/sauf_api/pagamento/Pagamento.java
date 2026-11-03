package br.com.sauf_api.pagamento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;

/**
 * Uma cobrança (invoice) do Stripe ligada a uma assinatura. Serve de histórico e para a
 * regra de reembolso integral em até 7 dias da primeira cobrança (CDC art. 49, ADR-BE-016).
 */
@Entity
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "assinatura_id", nullable = false)
    private UUID assinaturaId;

    @Column(name = "stripe_invoice_id", nullable = false, unique = true)
    private String stripeInvoiceId;

    @Column(name = "valor_centavos", nullable = false)
    private int valorCentavos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusPagamento status;

    @Column(name = "pago_em")
    private Instant pagoEm;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected Pagamento() {}

    public Pagamento(UUID assinaturaId, String stripeInvoiceId, int valorCentavos, StatusPagamento status, Instant pagoEm) {
        this.assinaturaId = assinaturaId;
        this.stripeInvoiceId = stripeInvoiceId;
        this.valorCentavos = valorCentavos;
        this.status = status;
        this.pagoEm = pagoEm;
        this.criadoEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getAssinaturaId() {
        return assinaturaId;
    }

    public String getStripeInvoiceId() {
        return stripeInvoiceId;
    }

    public int getValorCentavos() {
        return valorCentavos;
    }

    public StatusPagamento getStatus() {
        return status;
    }

    public Instant getPagoEm() {
        return pagoEm;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
