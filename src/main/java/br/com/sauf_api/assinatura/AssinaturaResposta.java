package br.com.sauf_api.assinatura;

import java.time.LocalDate;

public record AssinaturaResposta(Plano plano, StatusAssinatura status, LocalDate renovaEm) {

    public static AssinaturaResposta de(Assinatura assinatura) {
        return new AssinaturaResposta(assinatura.getPlano(), assinatura.getStatus(), assinatura.getRenovaEm());
    }
}
