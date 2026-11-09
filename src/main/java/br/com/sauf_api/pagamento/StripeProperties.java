package br.com.sauf_api.pagamento;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do Stripe (ADR-BE-016), lida de {@code sauf.stripe.*} no application.yml.
 * Sem a {@code secretKey}, os pagamentos ficam desabilitados e o resto da API sobe normalmente.
 */
@ConfigurationProperties("sauf.stripe")
public record StripeProperties(
        String secretKey,
        String webhookSecret,
        Precos precos,
        String urlSucesso,
        String urlCancelamento,
        String urlRetornoPortal) {

    public record Precos(String basic, String plus, String premium) {}

    public boolean habilitado() {
        return secretKey != null && !secretKey.isBlank();
    }
}
