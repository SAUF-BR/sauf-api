package br.com.sauf_api.assinatura;

import static br.com.sauf_api.assinatura.RecursoPlano.DASHBOARD;
import static br.com.sauf_api.assinatura.RecursoPlano.RANKING;
import static br.com.sauf_api.assinatura.RecursoPlano.SIMULADOS_ENEM;
import static br.com.sauf_api.assinatura.RecursoPlano.SIMULADOS_VESTIBULARES;
import static br.com.sauf_api.assinatura.RecursoPlano.TIMER;

import java.math.BigDecimal;
import java.util.List;
import br.com.sauf_api.comum.EnumComValor;

public enum Plano implements EnumComValor{
    BASIC("basic", "Basic", "8.00",
        "Para quem está começando a treinar para o ENEM.",
        null,
        SIMULADOS_ENEM),
    PLUS("plus", "Plus", "12.00",
        "Para quem também vai prestar vestibulares próprios.",
        null,
        SIMULADOS_ENEM, SIMULADOS_VESTIBULARES),
    PREMIUM("premium", "Premium", "15.00",
        "Para quem quer acompanhar a própria evolução de perto.",
        "Completo",
        SIMULADOS_ENEM, SIMULADOS_VESTIBULARES, DASHBOARD, TIMER, RANKING);

    private final String codigo;
    private final String nome;
    private final BigDecimal precoMensal;
    private final String descricao;
    private final String selo;
    private final List<RecursoPlano> recursos;

    Plano(String codigo, String nome, String precoMensal, String descricao, String selo, RecursoPlano... recursos){
        this.codigo = codigo;
        this.nome = nome;
        this.precoMensal = new BigDecimal(precoMensal);
        this.descricao = descricao;
        this.selo = selo;
        this.recursos = List.of(recursos);
    }

    @Override 
    public String valor(){
        return codigo;
    }

    public String getNome(){
        return nome;
    }

    public BigDecimal getPrecoMensal(){
        return precoMensal;
    }

    public String getDescricao(){
        return descricao;
    }

    public String getSelo(){
        return selo;
    }

    public List<RecursoPlano> getRecursos(){
        return recursos;
    }
}