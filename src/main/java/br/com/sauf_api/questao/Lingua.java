package br.com.sauf_api.questao;

import br.com.sauf_api.comum.EnumComValor;

/** Língua estrangeira da questão; null quando não é de língua estrangeira. */
public enum Lingua implements EnumComValor {
    INGLES("ingles"),
    ESPANHOL("espanhol"),
    FRANCES("frances");

    private final String valor;

    Lingua(String valor) {
        this.valor = valor;
    }

    @Override
    public String valor() {
        return valor;
    }
}
