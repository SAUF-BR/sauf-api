package br.com.sauf_api.questao;

import br.com.sauf_api.comum.EnumComValor;

/** Quem aplica a prova. */
public enum Banca implements EnumComValor {
    ENEM("enem"),
    UEM("uem");

    private final String valor;

    Banca(String valor) {
        this.valor = valor;
    }

    @Override
    public String valor() {
        return valor;
    }
}
