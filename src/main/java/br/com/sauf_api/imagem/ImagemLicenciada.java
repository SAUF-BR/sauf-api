package br.com.sauf_api.imagem;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;

/** Arquivo de imagem guardado no banco com o crédito que a licença pede (D-99, D-100). */
@MappedSuperclass
public abstract class ImagemLicenciada {

    @Column(nullable = false)
    private byte[] conteudo;

    @Column(name = "tipo_mime", nullable = false, length = 30)
    private String tipoMime;

    @Column(nullable = false, length = 64)
    private String hash;

    @Column(nullable = false, length = 300)
    private String autor;

    @Column(nullable = false, length = 100)
    private String licenca;

    @Column(name = "licenca_url", length = 500)
    private String licencaUrl;

    @Column(name = "fonte_url", nullable = false, length = 500)
    private String fonteUrl;

    @Column(name = "atualizada_em", nullable = false)
    private Instant atualizadaEm;

    /** Troca o arquivo e o crédito. Devolve false quando nada mudou (reenvio idêntico). */
    boolean trocar(ArquivoImagem arquivo, CreditoImagem credito) {
        if (arquivo.hash().equals(hash) && credito.equals(credito())) {
            return false;
        }
        this.conteudo = arquivo.conteudo();
        this.tipoMime = arquivo.tipoMime();
        this.hash = arquivo.hash();
        this.autor = credito.autor();
        this.licenca = credito.licenca();
        this.licencaUrl = credito.licencaUrl();
        this.fonteUrl = credito.fonteUrl();
        this.atualizadaEm = Instant.now();
        return true;
    }

    public CreditoImagem credito() {
        return new CreditoImagem(autor, licenca, licencaUrl, fonteUrl);
    }

    public byte[] getConteudo() {
        return conteudo;
    }

    public String getTipoMime() {
        return tipoMime;
    }

    public String getHash() {
        return hash;
    }
}
