package br.com.sauf_api.questao;

import br.com.sauf_api.comum.EnumComValor;

/** MULTIPLA_ESCOLHA: uma alternativa correta (A a E). SOMATORIA: afirmativas 01 a 16, resposta é a soma. */
public enum TipoQuestao implements EnumComValor {
    MULTIPLA_ESCOLHA("multipla_escolha"),
    SOMATORIA("somatoria");

    private final String valor;

    TipoQuestao(String valor) {
        this.valor = valor;
    }

    @Override
    public String valor() {
        return valor;
    }
}
