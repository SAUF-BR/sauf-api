package br.com.sauf_api.sisu;

import br.com.sauf_api.instituicao.Instituicao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Nota de corte do Sisu (ampla concorrência) de um curso e-MEC num turno e ano (D-50). */
@Entity
@Table(name = "nota_corte_sisu")
public class NotaCorteSisu {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instituicao_id")
    private Instituicao instituicao;

    @Column(name = "codigo_emec_curso", nullable = false)
    private Integer codigoEmecCurso;

    @Column(nullable = false, length = 20)
    private String turno;

    @Column(nullable = false)
    private Integer ano;

    private Integer vagas;

    private Integer inscricoes;

    @Column(precision = 6, scale = 2)
    private BigDecimal nota;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected NotaCorteSisu() {}

    public NotaCorteSisu(Instituicao instituicao, Integer codigoEmecCurso, String turno, Integer ano) {
        this.instituicao = instituicao;
        this.codigoEmecCurso = codigoEmecCurso;
        this.turno = turno;
        this.ano = ano;
    }

    public void atualizar(Integer vagas, Integer inscricoes, BigDecimal nota) {
        this.vagas = vagas;
        this.inscricoes = inscricoes;
        this.nota = nota;
        this.atualizadoEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Instituicao getInstituicao() {
        return instituicao;
    }

    public Integer getCodigoEmecCurso() {
        return codigoEmecCurso;
    }

    public String getTurno() {
        return turno;
    }

    public Integer getAno() {
        return ano;
    }

    public Integer getVagas() {
        return vagas;
    }

    public Integer getInscricoes() {
        return inscricoes;
    }

    public BigDecimal getNota() {
        return nota;
    }
}
