package br.com.sauf_api.simulado;

import br.com.sauf_api.comum.EnumComValor;

/** Janela das estatísticas do cronômetro, em dias até hoje. */
public enum PeriodoCronometro implements EnumComValor {
    SEMANA("semana", 7),
    MES("mes", 30),
    ANO("ano", 365);

    private final String valor;
    private final int dias;

    PeriodoCronometro(String valor, int dias) {
        this.valor = valor;
        this.dias = dias;
    }

    @Override
    public String valor() {
        return valor;
    }

    public int dias() {
        return dias;
    }
}
