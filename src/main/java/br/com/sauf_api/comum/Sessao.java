package br.com.sauf_api.comum;

import java.time.Instant;
import java.util.UUID;

/**
 * Quem está na requisição, lido do cookie da sessão (D-84). {@code trocaSenhaSemAtualAte}
 * vem preenchido quando a pessoa entrou pelo código do "esqueci minha senha" (D-90): até
 * esse instante ela pode criar uma senha nova sem informar a atual.
 */
public record Sessao(UUID usuarioId, Instant trocaSenhaSemAtualAte) {

    public boolean podeTrocarSenhaSemAtual(Instant agora) {
        return trocaSenhaSemAtualAte != null && agora.isBefore(trocaSenhaSemAtualAte);
    }
}
