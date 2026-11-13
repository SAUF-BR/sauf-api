package br.com.sauf_api.simulado;

import br.com.sauf_api.comum.EnumComValor;

/** Tamanho do simulado e número de questões. */
public enum TamanhoSimulado implements EnumComValor {
    RAPIDA("rapida", 10),
    MEDIA("media", 20),
    LONGA("longa", 30);

    private final String valor;
    private final int questoes;

    TamanhoSimulado(String valor, int questoes) {
        this.valor = valor;
        this.questoes = questoes;
    }

    @Override
    public String valor() {
        return valor;
    }

    public int questoes() {
        return questoes;
    }
}
