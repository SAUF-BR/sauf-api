package br.com.sauf_api.assinatura;

import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Ponto único para conferir se o plano do usuário inclui um recurso (D-98, D-109). Quem não
 * tem assinatura vigente está no Free.
 */
@Component
public class AcessoRecursos {

    private final AssinaturaService assinaturas;

    public AcessoRecursos(AssinaturaService assinaturas) {
        this.assinaturas = assinaturas;
    }

    public void exigir(UUID usuarioId, RecursoPlano recurso) {
        if (!assinaturas.planoAtual(usuarioId).getRecursos().contains(recurso)) {
            throw new RecursoForaDoPlanoException();
        }
    }
}
