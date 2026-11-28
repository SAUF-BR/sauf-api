package br.com.sauf_api.pagamento;

import br.com.sauf_api.assinatura.Plano;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotNull(message = "Escolha um plano") Plano plano,
        @NotNull(message = "Aceite o termo para continuar")
        @AssertTrue(message = "Aceite o termo para continuar")
        Boolean aceiteTermo) {}
