package br.com.sauf_api.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lê o cookie da sessão e, se o token for válido, marca a requisição como autenticada
 * (o principal é a Sessao: usuário e se pode trocar a senha sem a atual). Token ausente, vencido ou adulterado = visitante:
 * as rotas públicas seguem funcionando e as protegidas respondem 401.
 * Não é um @Component de propósito: só roda dentro da cadeia do Spring Security.
 */
class FiltroSessao extends OncePerRequestFilter {

    private final TokenService tokens;

    FiltroSessao(TokenService tokens) {
        this.tokens = tokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        CookieSessao.ler(request)
                .flatMap(tokens::validar)
                .ifPresent(sessao -> SecurityContextHolder.getContext().setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(sessao, null, List.of())));
        chain.doFilter(request, response);
    }
}
