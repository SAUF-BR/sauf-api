package br.com.sauf_api.assinatura;

import br.com.sauf_api.comum.EnumComValor;

public enum RecursoPlano implements EnumComValor {
    BUSCA_CURSOS("busca-cursos"),
    BUSCA_UNIVERSIDADES("busca-universidades"),
    TESTE_VOCACIONAL("teste-vocacional"),
    CALENDARIO("calendario"),
    FAVORITOS("favoritos"),
    SIMULADOS_ENEM("simulados-enem"),
    RANKING("ranking"),
    TIMER("timer"),
    DASHBOARD("dashboard");

    private final String valor;

    RecursoPlano(String valor) {
        this.valor = valor;
    }

    @Override
    public String valor() {
        return valor;
    }
}
