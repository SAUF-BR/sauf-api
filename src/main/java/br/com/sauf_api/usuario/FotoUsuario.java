package br.com.sauf_api.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuario_foto")
public class FotoUsuario {

    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(nullable = false)
    private byte[] conteudo;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(name = "atualizada_em", nullable = false)
    private Instant atualizadaEm;

    protected FotoUsuario() {}

    public FotoUsuario(UUID usuarioId, byte[] conteudo, String tipo) {
        this.usuarioId = usuarioId;
        trocar(conteudo, tipo);
    }

    public void trocar(byte[] conteudo, String tipo) {
        this.conteudo = conteudo;
        this.tipo = tipo;
        this.atualizadaEm = Instant.now();
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public byte[] getConteudo() {
        return conteudo;
    }

    public String getTipo() {
        return tipo;
    }

    public Instant getAtualizadaEm() {
        return atualizadaEm;
    }
}
