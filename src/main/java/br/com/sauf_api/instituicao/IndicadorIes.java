package br.com.sauf_api.instituicao;

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

/** Indicadores de qualidade da IES num ano (hoje só o IGC). Um registro por ano, sem apagar os anteriores (D-49). */
@Entity
@Table(name = "indicador_ies")
public class IndicadorIes {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instituicao_id")
    private Instituicao instituicao;

    @Column(nullable = false)
    private Integer ano;

    @Column(name = "igc_continuo", precision = 5, scale = 3)
    private BigDecimal igcContinuo;

    @Column(name = "igc_faixa")
    private Integer igcFaixa;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected IndicadorIes() {}

    public IndicadorIes(Instituicao instituicao, Integer ano) {
        this.instituicao = instituicao;
        this.ano = ano;
    }

    public void atualizarIgc(BigDecimal igcContinuo, Integer igcFaixa) {
        this.igcContinuo = igcContinuo;
        this.igcFaixa = igcFaixa;
        this.atualizadoEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Instituicao getInstituicao() {
        return instituicao;
    }

    public Integer getAno() {
        return ano;
    }

    public BigDecimal getIgcContinuo() {
        return igcContinuo;
    }

    public Integer getIgcFaixa() {
        return igcFaixa;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
