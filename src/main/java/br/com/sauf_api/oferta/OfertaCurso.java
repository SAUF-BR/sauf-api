package br.com.sauf_api.oferta;

import br.com.sauf_api.curso.Curso;
import br.com.sauf_api.instituicao.Instituicao;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/** Uma página de curso no site da instituição (D-34). */
@Entity
@Table(name = "oferta_curso")
public class OfertaCurso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instituicao_id", nullable = false)
    private Instituicao instituicao;

    @Column(nullable = false)
    private String chave;

    @Column(name = "nome_no_site", nullable = false)
    private String nomeNoSite;

    @ElementCollection
    @CollectionTable(name = "oferta_opcao", joinColumns = @JoinColumn(name = "oferta_id"))
    private List<Opcao> opcoes = new ArrayList<>();

    // D-35: só para ligar a oferta ao Censo; não sai na API pública.
    @ElementCollection
    @CollectionTable(name = "oferta_codigo_emec", joinColumns = @JoinColumn(name = "oferta_id"))
    @Column(name = "codigo_emec", nullable = false)
    private Set<Integer> codigosEmec = new TreeSet<>();

    @Enumerated(EnumType.STRING)
    private Modalidade modalidade;

    private String turno;

    @Column(columnDefinition = "TEXT")
    private String sobre;

    @Column(name = "mercado_trabalho", columnDefinition = "TEXT")
    private String mercadoTrabalho;

    @Column(name = "duracao_texto")
    private String duracaoTexto;

    @Column(name = "duracao_semestres")
    private Integer duracaoSemestres;

    @Column(length = 2)
    private String uf;

    private String cidade;

    private Integer vagas;

    private Boolean gratuito;

    @Column(name = "url_origem")
    private String urlOrigem;

    @Column(precision = 10, scale = 2)
    private BigDecimal mensalidade;

    @Column(nullable = false)
    private boolean ativa = true;

    @Column(name = "coletada_em", nullable = false)
    private Instant coletadaEm;

    protected OfertaCurso() {}

    public OfertaCurso(Curso curso, Instituicao instituicao, String chave, String nomeNoSite) {
        this.curso = curso;
        this.instituicao = instituicao;
        this.chave = chave;
        this.nomeNoSite = nomeNoSite;
        this.coletadaEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Curso getCurso() {
        return curso;
    }

    public void setCurso(Curso curso) {
        this.curso = curso;
    }

    public Instituicao getInstituicao() {
        return instituicao;
    }

    public String getChave() {
        return chave;
    }

    public String getNomeNoSite() {
        return nomeNoSite;
    }

    public void setNomeNoSite(String nomeNoSite) {
        this.nomeNoSite = nomeNoSite;
    }

    public List<Opcao> getOpcoes() {
        return opcoes;
    }

    public void setOpcoes(Collection<Opcao> opcoes) {
        this.opcoes.clear();
        this.opcoes.addAll(opcoes);
    }

    public Set<Integer> getCodigosEmec() {
        return codigosEmec;
    }

    public void setCodigosEmec(Collection<Integer> codigosEmec) {
        this.codigosEmec.clear();
        this.codigosEmec.addAll(codigosEmec);
    }

    public Modalidade getModalidade() {
        return modalidade;
    }

    public void setModalidade(Modalidade modalidade) {
        this.modalidade = modalidade;
    }

    public String getTurno() {
        return turno;
    }

    public void setTurno(String turno) {
        this.turno = turno;
    }

    public String getSobre() {
        return sobre;
    }

    public void setSobre(String sobre) {
        this.sobre = sobre;
    }

    public String getMercadoTrabalho() {
        return mercadoTrabalho;
    }

    public void setMercadoTrabalho(String mercadoTrabalho) {
        this.mercadoTrabalho = mercadoTrabalho;
    }

    public String getDuracaoTexto() {
        return duracaoTexto;
    }

    public void setDuracaoTexto(String duracaoTexto) {
        this.duracaoTexto = duracaoTexto;
    }

    public Integer getDuracaoSemestres() {
        return duracaoSemestres;
    }

    public void setDuracaoSemestres(Integer duracaoSemestres) {
        this.duracaoSemestres = duracaoSemestres;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public Integer getVagas() {
        return vagas;
    }

    public void setVagas(Integer vagas) {
        this.vagas = vagas;
    }

    public Boolean getGratuito() {
        return gratuito;
    }

    public void setGratuito(Boolean gratuito) {
        this.gratuito = gratuito;
    }

    public String getUrlOrigem() {
        return urlOrigem;
    }

    public void setUrlOrigem(String urlOrigem) {
        this.urlOrigem = urlOrigem;
    }

    public BigDecimal getMensalidade() {
        return mensalidade;
    }

    public void setMensalidade(BigDecimal mensalidade) {
        this.mensalidade = mensalidade;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }

    public Instant getColetadaEm() {
        return coletadaEm;
    }

    public void marcarColetada(Instant coletadaEm) {
        this.ativa = true;
        this.coletadaEm = coletadaEm;
    }
}
