package br.com.sauf_api.questao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/** Texto de apoio em Markdown, compartilhado pelas questões da prova que usam o mesmo texto. */
@Entity
@Table(name = "texto_base")
public class TextoBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prova_id")
    private Prova prova;

    @Column(nullable = false)
    private String conteudo;

    @Column(nullable = false, length = 64)
    private String hash;

    protected TextoBase() {}

    public TextoBase(Prova prova, String conteudo, String hash) {
        this.prova = prova;
        this.conteudo = conteudo;
        this.hash = hash;
    }

    public UUID getId() {
        return id;
    }

    public Prova getProva() {
        return prova;
    }

    public String getConteudo() {
        return conteudo;
    }

    public String getHash() {
        return hash;
    }
}
