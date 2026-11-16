package br.com.sauf_api.assinatura;

import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Ponto único para conferir se o plano do usuário inclui um recurso (ex.: RANKING, só no Plus).
 * Provisório até o login (D-55, TR-336): libera tudo, como o UsuarioLogado.
 */
@Component
public class AcessoRecursos {

    public void exigir(UUID usuarioId, RecursoPlano recurso) {
        // Com o login: buscar a assinatura ativa e lançar AcaoNaoPermitidaException sem o recurso.
    }
}
