package br.com.sauf_api.questao;

import br.com.sauf_api.comum.EnumComValor;

/** Os simulados só sorteiam DISPONIVEL e não anulada (D-68). */
public enum StatusQuestao implements EnumComValor {
    DISPONIVEL("disponivel"),
    PENDENTE_REVISAO("pendente_revisao");

    private final String valor;

    StatusQuestao(String valor) {
        this.valor = valor;
    }

    @Override
    public String valor() {
        return valor;
    }
}
