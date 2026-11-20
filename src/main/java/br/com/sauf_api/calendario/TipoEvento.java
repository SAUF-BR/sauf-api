package br.com.sauf_api.calendario;

import br.com.sauf_api.comum.EnumComValor;

public enum TipoEvento implements EnumComValor {
    INSCRICAO("inscricao"),
    PROVA("prova"),
    RESULTADO("resultado"),
    FEIRA("feira"),
    OUTRO("outro");

    private final String valor;

    TipoEvento(String valor) {
        this.valor = valor;
    }

    @Override
    public String valor() {
        return valor;
    }
}
