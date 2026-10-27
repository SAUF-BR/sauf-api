package br.com.sauf_api.assinatura;

import jakarta.validation.constraints.NotNull;

public record TrocarPlanoRequest(@NotNull(message = "Escolha um plano") Plano plano) {}
