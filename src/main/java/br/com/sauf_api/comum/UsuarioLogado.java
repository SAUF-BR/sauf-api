package br.com.sauf_api.comum;

import java.time.Instant;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Quem está fazendo a requisição: a sessão do cookie (D-84), colocada no contexto pelo
 * FiltroSessao. Só deve ser usado em rotas que exigem login (SegurancaConfig); fora delas
 * não há usuário e a chamada responde 401.
 */
@Component
public class UsuarioLogado {

    public UUID id() {
        return sessao().usuarioId();
    }

    /** Entrou pelo código do "esqueci minha senha" há menos de 15 minutos (D-90). */
    public boolean podeTrocarSenhaSemAtual() {
        return sessao().podeTrocarSenhaSemAtual(Instant.now());
    }

    public Sessao sessao() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.getPrincipal() instanceof Sessao sessao) {
            return sessao;
        }
        throw new NaoAutenticadoException();
    }
}
