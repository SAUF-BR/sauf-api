package br.com.sauf_api.questao;

import br.com.sauf_api.comum.EnumComValor;

/** Área do conhecimento (as quatro do ENEM). */
public enum AreaQuestao implements EnumComValor {
    LC("lc"),
    CH("ch"),
    CN("cn"),
    MT("mt");

    private final String valor;

    AreaQuestao(String valor) {
        this.valor = valor;
    }

    @Override
    public String valor() {
        return valor;
    }
}
