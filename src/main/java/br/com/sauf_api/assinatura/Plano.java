package br.com.sauf_api.assinatura;

import static br.com.sauf_api.assinatura.RecursoPlano.BUSCA_CURSOS;
import static br.com.sauf_api.assinatura.RecursoPlano.BUSCA_UNIVERSIDADES;
import static br.com.sauf_api.assinatura.RecursoPlano.CALENDARIO;
import static br.com.sauf_api.assinatura.RecursoPlano.DASHBOARD;
import static br.com.sauf_api.assinatura.RecursoPlano.FAVORITOS;
import static br.com.sauf_api.assinatura.RecursoPlano.RANKING;
import static br.com.sauf_api.assinatura.RecursoPlano.SIMULADOS_ENEM;
import static br.com.sauf_api.assinatura.RecursoPlano.TESTE_VOCACIONAL;
import static br.com.sauf_api.assinatura.RecursoPlano.TIMER;

import br.com.sauf_api.comum.EnumComValor;
import java.math.BigDecimal;
import java.util.List;

/**
 * D-98: Free (gratuito), Basic e Plus. O Free é o plano de quem não tem assinatura:
 * aparece na lista de planos, mas nunca vira linha na tabela assinatura.
 */
public enum Plano implements EnumComValor {
    FREE("free", "Free", "0.00",
        "Para explorar cursos e universidades e organizar a sua escolha.",
        null,
        BUSCA_CURSOS, BUSCA_UNIVERSIDADES, TESTE_VOCACIONAL, CALENDARIO, FAVORITOS),
    BASIC("basic", "Basic", "8.00",
        "Para quem quer começar a treinar com os simulados do ENEM.",
        null,
        BUSCA_CURSOS, BUSCA_UNIVERSIDADES, TESTE_VOCACIONAL, CALENDARIO, FAVORITOS,
        SIMULADOS_ENEM),
    PLUS("plus", "Plus", "10.00",
        "Para quem quer acompanhar a própria evolução de perto.",
        "Completo",
        BUSCA_CURSOS, BUSCA_UNIVERSIDADES, TESTE_VOCACIONAL, CALENDARIO, FAVORITOS,
        SIMULADOS_ENEM, RANKING, TIMER, DASHBOARD);

    private final String codigo;
    private final String nome;
    private final BigDecimal precoMensal;
    private final String descricao;
    private final String selo;
    private final List<RecursoPlano> recursos;

    Plano(String codigo, String nome, String precoMensal, String descricao, String selo, RecursoPlano... recursos) {
        this.codigo = codigo;
        this.nome = nome;
        this.precoMensal = new BigDecimal(precoMensal);
        this.descricao = descricao;
        this.selo = selo;
        this.recursos = List.of(recursos);
    }

    @Override
    public String valor() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getPrecoMensal() {
        return precoMensal;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getSelo() {
        return selo;
    }

    public List<RecursoPlano> getRecursos() {
        return recursos;
    }

    public boolean gratuito() {
        return precoMensal.signum() == 0;
    }
}
