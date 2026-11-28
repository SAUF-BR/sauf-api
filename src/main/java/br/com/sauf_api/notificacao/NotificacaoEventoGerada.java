package br.com.sauf_api.notificacao;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Registro de que o aviso de um evento × janela × usuário já foi criado. Garante que o
 * gerador rodando várias vezes não duplica notificações.
 */
@Entity
@Table(name = "notificacao_evento_gerada")
public class NotificacaoEventoGerada {

    @EmbeddedId
    private Id id;

    @Column(name = "gerada_em", nullable = false)
    private Instant geradaEm;

    protected NotificacaoEventoGerada() {}

    public NotificacaoEventoGerada(UUID eventoId, UUID usuarioId, int janelaDias, Instant geradaEm) {
        this.id = new Id(eventoId, usuarioId, janelaDias);
        this.geradaEm = geradaEm;
    }

    public UUID getEventoId() {
        return id.eventoId;
    }

    public UUID getUsuarioId() {
        return id.usuarioId;
    }

    public int getJanelaDias() {
        return id.janelaDias;
    }

    public Instant getGeradaEm() {
        return geradaEm;
    }

    @Embeddable
    public static class Id implements Serializable {

        @Column(name = "evento_id", nullable = false)
        private UUID eventoId;

        @Column(name = "usuario_id", nullable = false)
        private UUID usuarioId;

        @Column(name = "janela_dias", nullable = false)
        private int janelaDias;

        protected Id() {}

        public Id(UUID eventoId, UUID usuarioId, int janelaDias) {
            this.eventoId = eventoId;
            this.usuarioId = usuarioId;
            this.janelaDias = janelaDias;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Id outra)) return false;
            return janelaDias == outra.janelaDias
                    && Objects.equals(eventoId, outra.eventoId)
                    && Objects.equals(usuarioId, outra.usuarioId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(eventoId, usuarioId, janelaDias);
        }
    }
}
