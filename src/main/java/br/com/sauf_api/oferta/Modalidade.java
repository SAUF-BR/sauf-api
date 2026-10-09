package br.com.sauf_api.oferta;

import br.com.sauf_api.comum.EnumComValor;

public enum Modalidade implements EnumComValor {
    PRESENCIAL("presencial"),
    SEMIPRESENCIAL("semipresencial"),
    EAD("ead");

    private final String valor;

    Modalidade(String valor) {
        this.valor = valor;
    }

    @Override
    public String valor() {
        return valor;
    }
}
