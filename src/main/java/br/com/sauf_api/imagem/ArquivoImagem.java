package br.com.sauf_api.imagem;

import br.com.sauf_api.comum.DadosInvalidosException;
import br.com.sauf_api.comum.TipoImagem;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;

/** Imagem já decodificada e conferida: tipo pelos primeiros bytes e hash SHA-256 para o ETag. */
public record ArquivoImagem(byte[] conteudo, String tipoMime, String hash) {

    public static final int TAMANHO_MAXIMO = 2 * 1024 * 1024;
    // Base64 cresce 4/3: acima disso nem decodifica
    private static final int BASE64_MAXIMO = (TAMANHO_MAXIMO / 3 + 1) * 4;

    public static ArquivoImagem deBase64(String base64, String campo) {
        if (base64 == null || base64.isBlank()) {
            throw new DadosInvalidosException(campo, "Envie o arquivo da imagem em base64.");
        }
        if (base64.length() > BASE64_MAXIMO) {
            throw new DadosInvalidosException(campo, "A imagem deve ter no máximo 2 MB.");
        }
        byte[] conteudo;
        try {
            conteudo = Base64.getDecoder().decode(base64.strip());
        } catch (IllegalArgumentException e) {
            throw new DadosInvalidosException(campo, "O arquivo não está em base64 válido.");
        }
        if (conteudo.length > TAMANHO_MAXIMO) {
            throw new DadosInvalidosException(campo, "A imagem deve ter no máximo 2 MB.");
        }
        String tipo = TipoImagem.detectar(conteudo)
                .orElseThrow(() -> new DadosInvalidosException(campo, "A imagem deve ser JPG, PNG ou WebP."));
        return new ArquivoImagem(conteudo, tipo, sha256(conteudo));
    }

    private static String sha256(byte[] conteudo) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(conteudo));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
