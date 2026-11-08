package br.com.sauf_api.questao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;

/** Um caderno de um dia de prova (ex.: ENEM 2024, D1, LARANJA). */
@Entity
public class Prova {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Banca banca;

    @Column(nullable = false)
    private Integer ano;

    @Column(nullable = false, length = 20)
    private String edicao;

    @Column(nullable = false, length = 30)
    private String caderno;

    @Column(name = "url_origem")
    private String urlOrigem;

    @Column(nullable = false)
    private String credito;

    @Column(name = "atualizada_em", nullable = false)
    private Instant atualizadaEm;

    protected Prova() {}

    public Prova(Banca banca, Integer ano, String edicao, String caderno) {
        this.banca = banca;
        this.ano = ano;
        this.edicao = edicao;
        this.caderno = caderno;
    }

    public void atualizar(String urlOrigem, String credito) {
        this.urlOrigem = urlOrigem;
        this.credito = credito;
        this.atualizadaEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Banca getBanca() {
        return banca;
    }

    public Integer getAno() {
        return ano;
    }

    public String getEdicao() {
        return edicao;
    }

    public String getCaderno() {
        return caderno;
    }

    public String getUrlOrigem() {
        return urlOrigem;
    }

    public String getCredito() {
        return credito;
    }
}
