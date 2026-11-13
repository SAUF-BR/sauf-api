package br.com.sauf_api.simulado;

import br.com.sauf_api.comum.EnumComValor;

/** Estado da resposta de uma questão; TEMPO_ESGOTADO só existe no simulado cronometrado. */
public enum SituacaoQuestao implements EnumComValor {
    PENDENTE("pendente"),
    RESPONDIDA("respondida"),
    EM_BRANCO("em_branco"),
    TEMPO_ESGOTADO("tempo_esgotado");

    private final String valor;

    SituacaoQuestao(String valor) {
        this.valor = valor;
    }

    @Override
    public String valor() {
        return valor;
    }
}
