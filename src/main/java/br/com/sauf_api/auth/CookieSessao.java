package br.com.sauf_api.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Cookie da sessão (ADR-005 do front, D-84): HttpOnly (o JavaScript não lê o token),
 * SameSite=Lax (proteção contra CSRF) e Secure em produção (SAUF_COOKIE_SECURE=true).
 */
@Component
public class CookieSessao {

    public static final String NOME = "sauf_sessao";
    private static final String CAMINHO = "/api";

    private final boolean seguro;

    public CookieSessao(@Value("${sauf.auth.cookie-secure:false}") boolean seguro) {
        this.seguro = seguro;
    }

    public ResponseCookie criar(String token, Duration duracao) {
        return base(token).maxAge(duracao).build();
    }

    public ResponseCookie apagar() {
        return base("").maxAge(Duration.ZERO).build();
    }

    public static Optional<String> ler(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies)
                .filter(cookie -> NOME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(valor -> !valor.isBlank())
                .findFirst();
    }

    private ResponseCookie.ResponseCookieBuilder base(String valor) {
        return ResponseCookie.from(NOME, valor)
                .httpOnly(true)
                .secure(seguro)
                .sameSite("Lax")
                .path(CAMINHO);
    }
}
