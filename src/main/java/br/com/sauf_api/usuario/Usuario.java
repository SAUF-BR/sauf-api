package br.com.sauf_api.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
public class Usuario {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 254)
    private String email;

    // Hash BCrypt (D-84). Null em contas sem senha, que não conseguem entrar.
    @Column(name = "senha_hash", length = 100)
    private String senhaHash;

    @Column(name = "email_verificado", nullable = false)
    private boolean emailVerificado;

    @Column(name = "nota_enem", precision = 6, scale = 2)
    private BigDecimal notaEnem;

    @Column(name = "senha_alterada_em")
    private Instant senhaAlteradaEm;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;


    protected Usuario() {}

    /** Conta nova do cadastro (D-85): já com senha; os aceites dos documentos ficam em aceite_documento (D-93). */
    public Usuario(String nome, String email, String senhaHash) {
        Instant agora = Instant.now();
        this.id = UUID.randomUUID();
        this.nome = nome.trim();
        this.email = normalizarEmail(email);
        this.senhaHash = senhaHash;
        this.senhaAlteradaEm = agora;
        this.criadoEm = agora;
    }

    /** Pela redefinição de senha (D-90), que prova o e-mail. A verificação no cadastro virá depois (D-89). */
    public void confirmarEmail() {
        this.emailVerificado = true;
    }

    public void trocarSenha(String novoHash) {
        this.senhaHash = novoHash;
        this.senhaAlteradaEm = Instant.now();
    }

    public void atualizarPerfil(String nome, BigDecimal notaEnem) {
        this.nome = nome.trim();
        this.notaEnem = notaEnem;
    }

    public static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public String getSenhaHash() {
        return senhaHash;
    }


    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public boolean isEmailVerificado() {
        return emailVerificado;
    }

    public BigDecimal getNotaEnem() {
        return notaEnem;
    }

    public Instant getSenhaAlteradaEm() {
        return senhaAlteradaEm;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
