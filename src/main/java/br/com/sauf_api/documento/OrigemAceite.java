package br.com.sauf_api.documento;

import jakarta.servlet.http.HttpServletRequest;

/** De onde veio o aceite (IP e navegador), guardado como prova (D-93). */
public record OrigemAceite(String ip, String userAgent) {

    private static final int TAMANHO_IP = 45;
    private static final int TAMANHO_USER_AGENT = 500;

    /** Atrás de proxy (deploy), o IP real vem no primeiro item do X-Forwarded-For. */
    public static OrigemAceite de(HttpServletRequest request) {
        String encaminhado = request.getHeader("X-Forwarded-For");
        String ip = encaminhado != null && !encaminhado.isBlank()
                ? encaminhado.split(",")[0].trim()
                : request.getRemoteAddr();
        return new OrigemAceite(cortar(ip == null ? "desconhecido" : ip, TAMANHO_IP),
                cortar(request.getHeader("User-Agent"), TAMANHO_USER_AGENT));
    }

    private static String cortar(String valor, int tamanho) {
        return valor == null || valor.length() <= tamanho ? valor : valor.substring(0, tamanho);
    }
}
