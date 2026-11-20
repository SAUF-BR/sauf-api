package br.com.sauf_api.calendario;

import br.com.sauf_api.instituicao.Instituicao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Data de vestibular, exame ou programa (D-104): um dia ({@code fim} nulo) ou um período. */
@Entity
@Table(name = "evento_calendario")
public class EventoCalendario {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String chave;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoEvento tipo;

    @Column(nullable = false)
    private LocalDate inicio;

    private LocalDate fim;

    // Nulo nos eventos nacionais (ENEM, ProUni)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instituicao_id")
    private Instituicao instituicao;

    @Column(length = 1000)
    private String descricao;

    @Column(name = "fonte_url", nullable = false, length = 500)
    private String fonteUrl;

    @Column(name = "ano_referencia", nullable = false)
    private int anoReferencia;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected EventoCalendario() {}

    EventoCalendario(String chave) {
        this.id = UUID.randomUUID();
        this.chave = chave;
    }

    /** Substitui os dados do evento; devolve false se já estava igual (reenvio sem mudança). */
    boolean aplicar(
            String titulo,
            TipoEvento tipo,
            LocalDate inicio,
            LocalDate fim,
            Instituicao instituicao,
            String descricao,
            String fonteUrl,
            int anoReferencia,
            Instant agora) {
        boolean igual = Objects.equals(this.titulo, titulo)
                && this.tipo == tipo
                && Objects.equals(this.inicio, inicio)
                && Objects.equals(this.fim, fim)
                && Objects.equals(idDe(this.instituicao), idDe(instituicao))
                && Objects.equals(this.descricao, descricao)
                && Objects.equals(this.fonteUrl, fonteUrl)
                && this.anoReferencia == anoReferencia;
        if (igual) {
            return false;
        }
        this.titulo = titulo;
        this.tipo = tipo;
        this.inicio = inicio;
        this.fim = fim;
        this.instituicao = instituicao;
        this.descricao = descricao;
        this.fonteUrl = fonteUrl;
        this.anoReferencia = anoReferencia;
        this.atualizadoEm = agora;
        return true;
    }

    /** O dia que conta como prazo: o fim do período ou o próprio dia do evento. */
    public LocalDate prazo() {
        return fim != null ? fim : inicio;
    }

    private static UUID idDe(Instituicao instituicao) {
        return instituicao == null ? null : instituicao.getId();
    }

    public UUID getId() {
        return id;
    }

    public String getChave() {
        return chave;
    }

    public String getTitulo() {
        return titulo;
    }

    public TipoEvento getTipo() {
        return tipo;
    }

    public LocalDate getInicio() {
        return inicio;
    }

    public LocalDate getFim() {
        return fim;
    }

    public Instituicao getInstituicao() {
        return instituicao;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getFonteUrl() {
        return fonteUrl;
    }

    public int getAnoReferencia() {
        return anoReferencia;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
