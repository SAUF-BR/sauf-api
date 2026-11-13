package br.com.sauf_api.simulado;

import br.com.sauf_api.comum.EnumComValor;

public enum StatusSimulado implements EnumComValor {
    EM_ANDAMENTO("em_andamento"),
    CONCLUIDO("concluido");

    private final String valor;

    StatusSimulado(String valor) {
        this.valor = valor;
    }

    @Override
    public String valor() {
        return valor;
    }
}
