package br.com.sauf_api.documento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

/** Registro de que a pessoa aceitou uma versão de um documento (D-93). Só inserção: prova LGPD. */
@Entity
@Table(name = "aceite_documento")
public class AceiteDocumento {

    static final String CANAL_WEB = "WEB";

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "documento_versao_id", nullable = false)
    private UUID documentoVersaoId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "conteudo_sha256", nullable = false, length = 64)
    private String conteudoSha256;

    @Column(name = "aceito_em", nullable = false)
    private Instant aceitoEm;

    @Column(nullable = false, length = 45)
    private String ip;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(nullable = false, length = 10)
    private String canal;

    protected AceiteDocumento() {}

    AceiteDocumento(UUID usuarioId, DocumentoLegalVersao documento, OrigemAceite origem) {
        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.documentoVersaoId = documento.getId();
        this.conteudoSha256 = documento.getConteudoSha256();
        this.aceitoEm = Instant.now();
        this.ip = origem.ip();
        this.userAgent = origem.userAgent();
        this.canal = CANAL_WEB;
    }

    public UUID getDocumentoVersaoId() {
        return documentoVersaoId;
    }

    public Instant getAceitoEm() {
        return aceitoEm;
    }
}
