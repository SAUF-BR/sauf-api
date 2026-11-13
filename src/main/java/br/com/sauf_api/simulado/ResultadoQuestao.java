package br.com.sauf_api.simulado;

import br.com.sauf_api.comum.EnumComValor;

/** Resultado de uma questão depois que o simulado é finalizado. */
public enum ResultadoQuestao implements EnumComValor {
    ACERTO("acerto"),
    ERRO("erro"),
    EM_BRANCO("em_branco");

    private final String valor;

    ResultadoQuestao(String valor) {
        this.valor = valor;
    }

    @Override
    public String valor() {
        return valor;
    }
}
