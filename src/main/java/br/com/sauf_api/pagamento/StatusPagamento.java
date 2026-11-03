package br.com.sauf_api.pagamento;

import br.com.sauf_api.comum.EnumComValor;

public enum StatusPagamento implements EnumComValor {
    PAGO("pago"),
    FALHOU("falhou"),
    REEMBOLSADO("reembolsado");

    private final String valor;

    StatusPagamento(String valor) {
        this.valor = valor;
    }

    @Override
    public String valor() {
        return valor;
    }
}
