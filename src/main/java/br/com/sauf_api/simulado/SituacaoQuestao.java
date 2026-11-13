package br.com.sauf_api.simulado;

import br.com.sauf_api.comum.EnumComValor;

/** Estado da resposta de uma questão; a resposta fora do tempo é marcada à parte (D-86). */
public enum SituacaoQuestao implements EnumComValor {
    PENDENTE("pendente"),
    RESPONDIDA("respondida"),
    EM_BRANCO("em_branco");

    private final String valor;

    SituacaoQuestao(String valor) {
        this.valor = valor;
    }

    @Override
    public String valor() {
        return valor;
    }
}
