package br.com.sauf_api.questao;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Alternativa {

    @Column(nullable = false, length = 2)
    private String rotulo;

    private Integer valor;

    @Column(nullable = false)
    private String texto;

    @Column(nullable = false)
    private boolean correta;

    protected Alternativa() {}

    public Alternativa(String rotulo, Integer valor, String texto, boolean correta) {
        this.rotulo = rotulo;
        this.valor = valor;
        this.texto = texto;
        this.correta = correta;
    }

    public String getRotulo() {
        return rotulo;
    }

    public Integer getValor() {
        return valor;
    }

    public String getTexto() {
        return texto;
    }

    public boolean isCorreta() {
        return correta;
    }
}
