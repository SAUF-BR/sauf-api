package br.com.sauf_api.usuario;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/** Descobre o tipo da imagem pelos primeiros bytes, sem confiar no Content-Type enviado. */
final class TipoImagem {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] RIFF = "RIFF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WEBP = "WEBP".getBytes(StandardCharsets.US_ASCII);

    private TipoImagem() {}

    static Optional<String> detectar(byte[] conteudo) {
        if (comecaCom(conteudo, 0, JPEG)) return Optional.of("image/jpeg");
        if (comecaCom(conteudo, 0, PNG)) return Optional.of("image/png");
        if (comecaCom(conteudo, 0, RIFF) && comecaCom(conteudo, 8, WEBP)) return Optional.of("image/webp");
        return Optional.empty();
    }

    private static boolean comecaCom(byte[] conteudo, int inicio, byte[] assinatura) {
        int fim = inicio + assinatura.length;
        return conteudo.length >= fim && Arrays.equals(conteudo, inicio, fim, assinatura, 0, assinatura.length);
    }
}
