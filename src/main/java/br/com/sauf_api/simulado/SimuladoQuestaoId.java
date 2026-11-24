package br.com.sauf_api.simulado;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class SimuladoQuestaoId implements Serializable {

    @Column(name = "simulado_id")
    private UUID simuladoId;

    @Column(name = "ordem")
    private int ordem;

    protected SimuladoQuestaoId() {}

    public SimuladoQuestaoId(UUID simuladoId, int ordem) {
        this.simuladoId = simuladoId;
        this.ordem = ordem;
    }

    public UUID getSimuladoId() {
        return simuladoId;
    }

    public int getOrdem() {
        return ordem;
    }

    @Override
    public boolean equals(Object outro) {
        return outro instanceof SimuladoQuestaoId id && ordem == id.ordem && Objects.equals(simuladoId, id.simuladoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(simuladoId, ordem);
    }
}
