package br.com.sauf_api.favorito;

import br.com.sauf_api.instituicao.Instituicao;
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
@Table(name = "favorito_universidade")
public class FavoritoUniversidade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instituicao_id", nullable = false)
    private Instituicao instituicao;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected FavoritoUniversidade() {}

    public FavoritoUniversidade(UUID usuarioId, Instituicao instituicao) {
        this.usuarioId = usuarioId;
        this.instituicao = instituicao;
        this.criadoEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public Instituicao getInstituicao() {
        return instituicao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}