package br.com.sauf_api.documento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Uma versão publicada de um documento legal (D-93), copiada do arquivo .md ao subir a API. */
@Entity
@Table(name = "documento_legal_versao")
public class DocumentoLegalVersao {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoDocumento tipo;

    @Column(nullable = false, length = 20)
    private String versao;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(name = "conteudo_markdown", nullable = false, columnDefinition = "TEXT")
    private String conteudoMarkdown;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "conteudo_sha256", nullable = false, length = 64)
    private String conteudoSha256;

    @Column(name = "resumo_alteracoes", columnDefinition = "TEXT")
    private String resumoAlteracoes;

    @Column(name = "exige_novo_aceite", nullable = false)
    private boolean exigeNovoAceite;

    @Column(name = "vigente_a_partir_de", nullable = false)
    private LocalDate vigenteAPartirDe;

    @Column(name = "publicada_em", nullable = false)
    private Instant publicadaEm;

    protected DocumentoLegalVersao() {}

    DocumentoLegalVersao(ArquivoDocumento arquivo) {
        this.id = UUID.randomUUID();
        this.tipo = arquivo.tipo();
        this.versao = arquivo.versao();
        this.publicadaEm = Instant.now();
        atualizar(arquivo);
    }

    /** Só para versões que ninguém aceitou ainda (DocumentosLegaisSincronizador). */
    void atualizar(ArquivoDocumento arquivo) {
        this.titulo = arquivo.titulo();
        this.conteudoMarkdown = arquivo.conteudo();
        this.conteudoSha256 = arquivo.sha256();
        this.resumoAlteracoes = arquivo.resumoAlteracoes();
        this.exigeNovoAceite = arquivo.exigeNovoAceite();
        this.vigenteAPartirDe = arquivo.vigenteAPartirDe();
    }

    public UUID getId() {
        return id;
    }

    public TipoDocumento getTipo() {
        return tipo;
    }

    public String getVersao() {
        return versao;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getConteudoMarkdown() {
        return conteudoMarkdown;
    }

    public String getConteudoSha256() {
        return conteudoSha256;
    }

    public String getResumoAlteracoes() {
        return resumoAlteracoes;
    }

    public boolean isExigeNovoAceite() {
        return exigeNovoAceite;
    }

    public LocalDate getVigenteAPartirDe() {
        return vigenteAPartirDe;
    }
}
