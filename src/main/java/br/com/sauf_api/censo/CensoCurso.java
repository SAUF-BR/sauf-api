package br.com.sauf_api.censo;

import br.com.sauf_api.curso.Curso;
import br.com.sauf_api.instituicao.Instituicao;
import br.com.sauf_api.oferta.Grau;
import br.com.sauf_api.oferta.Modalidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Um curso do Censo da Educação Superior. Só complementa as ofertas do site (D-33). */
@Entity
@Table(name = "censo_curso")
public class CensoCurso {

    @Id
    @Column(name = "codigo_emec")
    private Integer codigoEmec;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instituicao_id", nullable = false)
    private Instituicao instituicao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    private Grau grau;

    @Enumerated(EnumType.STRING)
    private Modalidade modalidade;

    private String cidade;

    @Column(length = 2)
    private String uf;

    private Integer vagas;

    private Boolean gratuito;

    @Column(name = "ano_censo", nullable = false)
    private Integer anoCenso;

    protected CensoCurso() {}

    public CensoCurso(Integer codigoEmec) {
        this.codigoEmec = codigoEmec;
    }

    public void atualizar(
            Instituicao instituicao,
            Curso curso,
            String nome,
            Grau grau,
            Modalidade modalidade,
            String cidade,
            String uf,
            Integer vagas,
            Boolean gratuito,
            Integer anoCenso) {
        this.instituicao = instituicao;
        this.curso = curso;
        this.nome = nome;
        this.grau = grau;
        this.modalidade = modalidade;
        this.cidade = cidade;
        this.uf = uf;
        this.vagas = vagas;
        this.gratuito = gratuito;
        this.anoCenso = anoCenso;
    }

    public Integer getCodigoEmec() {
        return codigoEmec;
    }

    public Instituicao getInstituicao() {
        return instituicao;
    }

    public Curso getCurso() {
        return curso;
    }

    public Grau getGrau() {
        return grau;
    }

    public Integer getVagas() {
        return vagas;
    }

    public Boolean getGratuito() {
        return gratuito;
    }
}
