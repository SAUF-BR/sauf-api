package br.com.sauf_api.instituicao;

import br.com.sauf_api.comum.EnumComValor;

public enum Esfera implements EnumComValor {
    FEDERAL("federal"),
    ESTADUAL("estadual"),
    MUNICIPAL("municipal");

    private final String valor;

    Esfera(String valor) {
        this.valor = valor;
    }

    @Override
    public String valor() {
        return valor;
    }
}