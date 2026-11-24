package br.com.sauf_api.favorito;

import br.com.sauf_api.oferta.OfertaCurso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "favorito_curso")
public class FavoritoCurso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "oferta_id", nullable = false)
    private OfertaCurso oferta;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected FavoritoCurso() {}

    public FavoritoCurso(UUID usuarioId, OfertaCurso oferta) {
        this.usuarioId = usuarioId;
        this.oferta = oferta;
        this.criadoEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public OfertaCurso getOferta() {
        return oferta;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}