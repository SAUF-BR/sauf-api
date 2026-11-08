package br.com.sauf_api.questao;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
public class Questao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prova_id")
    private Prova prova;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "texto_base_id")
    private TextoBase textoBase;

    @Column(nullable = false)
    private Integer numero;

    @Convert(converter = LinguaConverter.class)
    @Column(nullable = false, length = 10)
    private Lingua lingua;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoQuestao tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private AreaQuestao area;

    @Column(length = 50)
    private String materia;

    @Column(length = 100)
    private String assunto;

    private Integer habilidade;

    @Column(nullable = false)
    private String comando;

    @Column(name = "gabarito_soma")
    private Integer gabaritoSoma;

    @Column(precision = 8, scale = 5)
    private BigDecimal dificuldade;

    @Column(nullable = false)
    private boolean anulada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusQuestao status;

    @Column(name = "motivo_pendencia", length = 500)
    private String motivoPendencia;

    @Column(name = "codigo_origem")
    private Integer codigoOrigem;

    @Column(nullable = false)
    private String credito;

    @Column(name = "atualizada_em", nullable = false)
    private Instant atualizadaEm;

    @ElementCollection
    @CollectionTable(name = "alternativa", joinColumns = @JoinColumn(name = "questao_id"))
    private List<Alternativa> alternativas = new ArrayList<>();

    protected Questao() {}

    public Questao(Prova prova, Integer numero, Lingua lingua) {
        this.prova = prova;
        this.numero = numero;
        this.lingua = lingua;
    }

    /** Dados que vêm da fonte; a reimportação sobrescreve tudo, inclusive as alternativas. */
    public record Conteudo(
            TextoBase textoBase,
            TipoQuestao tipo,
            AreaQuestao area,
            String materia,
            String assunto,
            Integer habilidade,
            String comando,
            Integer gabaritoSoma,
            BigDecimal dificuldade,
            boolean anulada,
            StatusQuestao status,
            String motivoPendencia,
            Integer codigoOrigem,
            String credito,
            Collection<Alternativa> alternativas) {}

    public void atualizar(Conteudo conteudo) {
        this.textoBase = conteudo.textoBase();
        this.tipo = conteudo.tipo();
        this.area = conteudo.area();
        this.materia = conteudo.materia();
        this.assunto = conteudo.assunto();
        this.habilidade = conteudo.habilidade();
        this.comando = conteudo.comando();
        this.gabaritoSoma = conteudo.gabaritoSoma();
        this.dificuldade = conteudo.dificuldade();
        this.anulada = conteudo.anulada();
        this.status = conteudo.status();
        this.motivoPendencia = conteudo.motivoPendencia();
        this.codigoOrigem = conteudo.codigoOrigem();
        this.credito = conteudo.credito();
        this.alternativas.clear();
        this.alternativas.addAll(conteudo.alternativas());
        this.atualizadaEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Prova getProva() {
        return prova;
    }

    public TextoBase getTextoBase() {
        return textoBase;
    }

    public Integer getNumero() {
        return numero;
    }

    public Lingua getLingua() {
        return lingua;
    }

    public TipoQuestao getTipo() {
        return tipo;
    }

    public AreaQuestao getArea() {
        return area;
    }

    public String getMateria() {
        return materia;
    }

    public String getAssunto() {
        return assunto;
    }

    public Integer getHabilidade() {
        return habilidade;
    }

    public String getComando() {
        return comando;
    }

    public Integer getGabaritoSoma() {
        return gabaritoSoma;
    }

    public BigDecimal getDificuldade() {
        return dificuldade;
    }

    public boolean isAnulada() {
        return anulada;
    }

    public StatusQuestao getStatus() {
        return status;
    }

    public String getMotivoPendencia() {
        return motivoPendencia;
    }

    public Integer getCodigoOrigem() {
        return codigoOrigem;
    }

    public String getCredito() {
        return credito;
    }

    public List<Alternativa> getAlternativas() {
        return alternativas;
    }
}
