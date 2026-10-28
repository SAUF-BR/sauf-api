package br.com.sauf_api.instituicao;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;

@Entity
public class Instituicao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "codigo_emec", nullable = false, unique = true)
    private Integer codigoEmec;

    @Column(nullable = false)
    private String nome;

    private String sigla;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoInstituicao tipo;

    @Column(name = "categoria_administrativa")
    private String categoriaAdministrativa;

    @Column(name = "organizacao_academica")
    private String organizacaoAcademica;

    @Column(nullable = false, length = 2)
    private String uf;

    @Column(nullable = false)
    private String cidade;

    @Column(name = "site_url")
    private String siteUrl;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "nota_mec")
    private Integer notaMec;

    @Embedded
    private Endereco endereco;

    @Column(nullable = false)
    private boolean ativa = true;

    @Column(name = "atualizada_em", nullable = false)
    private Instant atualizadaEm;

    protected Instituicao() {}

    public Instituicao(Integer codigoEmec, String nome, TipoInstituicao tipo, String uf, String cidade) {
        this.codigoEmec = codigoEmec;
        this.nome = nome;
        this.tipo = tipo;
        this.uf = uf;
        this.cidade = cidade;
        this.atualizadaEm = Instant.now();
    }

    public void atualizarDoCenso(
            String nome,
            String sigla,
            TipoInstituicao tipo,
            String categoriaAdministrativa,
            String organizacaoAcademica,
            String uf,
            String cidade,
            Endereco endereco) {
        this.nome = nome;
        this.sigla = sigla;
        this.tipo = tipo;
        this.categoriaAdministrativa = categoriaAdministrativa;
        this.organizacaoAcademica = organizacaoAcademica;
        this.uf = uf;
        this.cidade = cidade;
        this.endereco = endereco;
        this.atualizadaEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Integer getCodigoEmec() {
        return codigoEmec;
    }

    public String getNome() {
        return nome;
    }

    public String getSigla() {
        return sigla;
    }

    public void setSigla(String sigla) {
        this.sigla = sigla;
    }

    public TipoInstituicao getTipo() {
        return tipo;
    }

    public String getCategoriaAdministrativa() {
        return categoriaAdministrativa;
    }

    public void setCategoriaAdministrativa(String categoriaAdministrativa) {
        this.categoriaAdministrativa = categoriaAdministrativa;
    }

    public String getOrganizacaoAcademica() {
        return organizacaoAcademica;
    }

    public void setOrganizacaoAcademica(String organizacaoAcademica) {
        this.organizacaoAcademica = organizacaoAcademica;
    }

    public String getUf() {
        return uf;
    }

    public String getCidade() {
        return cidade;
    }

    public String getSiteUrl() {
        return siteUrl;
    }

    public void setSiteUrl(String siteUrl) {
        this.siteUrl = siteUrl;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    /** Pode ser null: o Hibernate devolve null quando todas as colunas do endereço estão vazias!! */
    public Endereco getEndereco() {
        return endereco;
    }

    public Integer getNotaMec() {
        return notaMec;
    }

    public void setNotaMec(Integer notaMec) {
        this.notaMec = notaMec;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }

    public Instant getAtualizadaEm() {
        return atualizadaEm;
    }}
