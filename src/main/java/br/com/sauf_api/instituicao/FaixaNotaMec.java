package br.com.sauf_api.instituicao;

import br.com.sauf_api.comum.EnumComValor;

public enum FaixaNotaMec implements EnumComValor {
    QUATRO_OU_CINCO("4-5", 4, 5),
    TRES("3", 3, 3);

    private final String valor;
    private final int minima;
    private final int maxima;

    FaixaNotaMec(String valor, int minima, int maxima) {
        this.valor = valor;
        this.minima = minima;
        this.maxima = maxima;
    }

    @Override
    public String valor() {
        return valor;
    }

    public int minima() {
        return minima;
    }

    public int maxima() {
        return maxima;
    }
}