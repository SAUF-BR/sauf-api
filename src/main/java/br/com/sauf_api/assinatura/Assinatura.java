package br.com.sauf_api.assinatura;

import br.com.sauf_api.comum.AcaoNaoPermitidaException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
public class Assinatura {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plano plano;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAssinatura status;

    @Column(name = "renova_em", nullable = false)
    private LocalDate renovaEm;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    protected Assinatura() {}

    public Assinatura(UUID usuarioId, Plano plano, LocalDate renovaEm) {
        this.usuarioId = usuarioId;
        this.plano = plano;
        this.status = StatusAssinatura.ATIVA;
        this.renovaEm = renovaEm;
        this.criadaEm = Instant.now();
    }

    // D-57: sem pagamento confirmado, o acesso vai até o dia do renovaEm, inclusive.
    public boolean vigente(LocalDate hoje) {
        return !hoje.isAfter(renovaEm);
    }

    public void trocarPlano(Plano novo) {
        if (novo == plano && status == StatusAssinatura.ATIVA) {
            throw new AcaoNaoPermitidaException("Você já está no plano " + plano.getNome());
        }
        this.plano = novo;
        this.status = StatusAssinatura.ATIVA;
    }

    public void cancelar() {
        if (status != StatusAssinatura.ATIVA) {
            throw new AcaoNaoPermitidaException("O cancelamento já está agendado");
        }
        this.status = StatusAssinatura.CANCELAMENTO_AGENDADO;
    }

    public void reativar() {
        if (status != StatusAssinatura.CANCELAMENTO_AGENDADO) {
            throw new AcaoNaoPermitidaException("A assinatura já está ativa");
        }
        this.status = StatusAssinatura.ATIVA;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public Plano getPlano() {
        return plano;
    }

    public StatusAssinatura getStatus() {
        return status;
    }

    public LocalDate getRenovaEm() {
        return renovaEm;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }
}
