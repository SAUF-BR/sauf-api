package br.com.sauf_api.instituicao;

import br.com.sauf_api.comum.EnumComValor;

public enum TipoInstituicao implements EnumComValor {
    PUBLICA("publica"),
    PRIVADA("privada");

    private final String valor;

    TipoInstituicao(String valor) {
        this.valor = valor;
    }

    @Override
    public String valor() {
        return valor;
    }
}
