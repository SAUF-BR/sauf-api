package br.com.sauf_api.simulado;

import br.com.sauf_api.comum.EnumComValor;

/** Ordenação da lista de simulados. */
public enum OrdemSimulados implements EnumComValor {
    RECENTE("recente"),
    ANTIGO("antigo"),
    MAIOR_NOTA("maior_nota"),
    MENOR_NOTA("menor_nota");

    private final String valor;

    OrdemSimulados(String valor) {
        this.valor = valor;
    }

    @Override
    public String valor() {
        return valor;
    }
}
