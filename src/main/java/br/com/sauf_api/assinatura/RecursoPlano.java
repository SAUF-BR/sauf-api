package br.com.sauf_api.assinatura;

import br.com.sauf_api.comum.EnumComValor;

public enum RecursoPlano implements EnumComValor{
    SIMULADOS_ENEM ("simulados-enem"),
    SIMULADOS_VESTIBULARES ("simulados-vestibulares"),
    DASHBOARD ("dashboard"),
    TIMER ("timer"),
    RANKING ("ranking");

    private final String valor;

    RecursoPlano(String valor){
        this.valor = valor;
    }

    @Override 
    public String valor(){
        return valor;
    }
}