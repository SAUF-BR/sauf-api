package br.com.sauf_api.admin;

import br.com.sauf_api.comum.ErroResposta;
import br.com.sauf_api.comum.Sessao;
import br.com.sauf_api.ingestao.ApiKeyInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.json.JsonMapper;

/**
 * Rotas /api/v1/admin/** (D-107): entra quem manda a X-Api-Key (o scraper) ou quem está logado
 * com uma conta ADMIN (as futuras telas de admin). Sem nenhum dos dois: 401; logado sem ser
 * admin: 403.
 */
public class AcessoAdminInterceptor implements HandlerInterceptor {

    private final ApiKeyInterceptor chave;
    private final AcessoAdmin acesso;
    private final JsonMapper json;

    public AcessoAdminInterceptor(ApiKeyInterceptor chave, AcessoAdmin acesso, JsonMapper json) {
        this.chave = chave;
        this.acesso = acesso;
        this.json = json;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (chave.chaveValida(request)) {
            return true;
        }
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.getPrincipal() instanceof Sessao sessao) {
            if (acesso.ehAdmin(sessao.usuarioId())) {
                return true;
            }
            return negar(response, HttpStatus.FORBIDDEN, "Acesso restrito a administradores");
        }
        return negar(response, HttpStatus.UNAUTHORIZED,
                "Entre com uma conta de administrador ou envie o header " + ApiKeyInterceptor.HEADER);
    }

    private boolean negar(HttpServletResponse response, HttpStatus status, String mensagem) throws Exception {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        json.writeValue(response.getWriter(), new ErroResposta(status.value(), status.getReasonPhrase(), mensagem, Map.of()));
        return false;
    }
}
