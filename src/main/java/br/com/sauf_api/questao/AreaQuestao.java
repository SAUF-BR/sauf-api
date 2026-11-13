package br.com.sauf_api.questao;

import br.com.sauf_api.comum.EnumComValor;

/** Área do conhecimento (as quatro do ENEM). */
public enum AreaQuestao implements EnumComValor {
    LC("lc", "Linguagens"),
    CH("ch", "Ciências Humanas"),
    CN("cn", "Ciências da Natureza"),
    MT("mt", "Matemática");

    private final String valor;
    private final String nome;

    AreaQuestao(String valor, String nome) {
        this.valor = valor;
        this.nome = nome;
    }

    public String nome() {
        return nome;
    }

    @Override
    public String valor() {
        return valor;
    }
}
