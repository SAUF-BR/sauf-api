package br.com.sauf_api.notificacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;

@Entity
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(nullable = false, length = 500)
    private String descricao;

    // rotulo e para são opcionais mas andam juntos (CHECK na migration).
    @Column(name = "acao_rotulo", length = 80)
    private String acaoRotulo;

    @Column(name = "acao_para", length = 500)
    private String acaoPara;

    @Column(nullable = false)
    private boolean lida;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "lida_em")
    private Instant lidaEm;

    protected Notificacao() {}

    public Notificacao(UUID usuarioId, String titulo, String descricao) {
        this(usuarioId, titulo, descricao, null, null);
    }

    public Notificacao(UUID usuarioId, String titulo, String descricao, String acaoRotulo, String acaoPara) {
        this.usuarioId = usuarioId;
        this.titulo = titulo;
        this.descricao = descricao;
        this.acaoRotulo = acaoRotulo;
        this.acaoPara = acaoPara;
        this.lida = false;
        this.criadaEm = Instant.now();
    }

    public void marcarComoLida(Instant momento) {
        if (!lida) {
            this.lida = true;
            this.lidaEm = momento;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getAcaoRotulo() {
        return acaoRotulo;
    }

    public String getAcaoPara() {
        return acaoPara;
    }

    public boolean isLida() {
        return lida;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getLidaEm() {
        return lidaEm;
    }
}
