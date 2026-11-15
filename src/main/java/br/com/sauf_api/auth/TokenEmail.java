package br.com.sauf_api.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Código enviado por e-mail (D-90). Guarda só o hash BCrypt do código. */
@Entity
@Table(name = "token_email")
public class TokenEmail {

    static final int MAXIMO_TENTATIVAS = 5;

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoTokenEmail tipo;

    @Column(nullable = false, length = 64)
    private String hash;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "usado_em")
    private Instant usadoEm;

    @Column(nullable = false)
    private int tentativas;

    protected TokenEmail() {}

    public TokenEmail(UUID usuarioId, TipoTokenEmail tipo, String hash, Instant agora) {
        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.hash = hash;
        this.criadoEm = agora;
        this.expiraEm = agora.plus(tipo.validade());
    }

    boolean vale(Instant agora) {
        return usadoEm == null && agora.isBefore(expiraEm) && tentativas < MAXIMO_TENTATIVAS;
    }

    /** Conta uma tentativa errada; devolve quantas ainda restam. */
    int errar() {
        tentativas++;
        return Math.max(0, MAXIMO_TENTATIVAS - tentativas);
    }

    void usar(Instant agora) {
        this.usadoEm = agora;
    }

    String getHash() {
        return hash;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public TipoTokenEmail getTipo() {
        return tipo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
