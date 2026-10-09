package br.com.sauf_api.oferta;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class Opcao {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Grau grau;

    private String turno;

    protected Opcao() {}

    public Opcao(Grau grau, String turno) {
        this.grau = grau;
        this.turno = turno;
    }

    public Grau getGrau() {
        return grau;
    }

    public String getTurno() {
        return turno;
    }
}
