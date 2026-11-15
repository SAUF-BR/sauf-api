package br.com.sauf_api;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import br.com.sauf_api.auth.CookieSessao;
import br.com.sauf_api.auth.TokenService;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Ajuda dos testes de rotas que exigem login (D-87). */
final class Sessao {

    /** Usuário de teste cadastrado pela migration V2026_11_09_2345__usuario.sql (sem senha). */
    static final UUID USUARIO_TESTE = UUID.fromString("f9def6d7-0717-4540-8ad0-ec03e301b2c7");

    private Sessao() {}

    static Cookie cookie(TokenService tokens, UUID usuarioId) {
        return new Cookie(CookieSessao.NOME, tokens.gerar(usuarioId));
    }

    /** MockMvc em que toda requisição já vai com o cookie de sessão do usuário de teste. */
    static MockMvc mvcLogado(WebApplicationContext contexto, TokenService tokens) {
        return mvcLogado(contexto, tokens, USUARIO_TESTE);
    }

    static MockMvc mvcLogado(WebApplicationContext contexto, TokenService tokens, UUID usuarioId) {
        return MockMvcBuilders.webAppContextSetup(contexto)
                .apply(springSecurity())
                .defaultRequest(get("/").cookie(cookie(tokens, usuarioId)))
                .build();
    }
}
