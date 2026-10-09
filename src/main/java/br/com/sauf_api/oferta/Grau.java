package br.com.sauf_api.oferta;

import br.com.sauf_api.comum.EnumComValor;

public enum Grau implements EnumComValor {
    BACHARELADO("bacharelado"),
    LICENCIATURA("licenciatura"),
    TECNOLOGICO("tecnologo"),
    BACHARELADO_E_LICENCIATURA("bacharelado_e_licenciatura");

    private final String valor;

    Grau(String valor) {
        this.valor = valor;
    }

    @Override
    public String valor() {
        return valor;
    }
}
