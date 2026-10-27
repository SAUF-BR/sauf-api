package br.com.sauf_api.assinatura;

import br.com.sauf_api.comum.EnumComValor;

public enum StatusAssinatura implements EnumComValor {
    ATIVA("ativa"),
    CANCELAMENTO_AGENDADO("cancelamento-agendado");

    private final String valor;

    StatusAssinatura(String valor) {
        this.valor = valor;
    }

    @Override
    public String valor() {
        return valor;
    }
}
