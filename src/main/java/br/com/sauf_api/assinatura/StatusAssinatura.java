package br.com.sauf_api.assinatura;

import br.com.sauf_api.comum.EnumComValor;

/**
 * Status da assinatura, espelhando o {@code subscription.status} do Stripe (ADR-BE-016).
 * {@code CANCELAMENTO_AGENDADO} é o "active com cancel_at_period_end" do Stripe.
 * O banco guarda o nome da constante: não renomeie as que já existem.
 */
public enum StatusAssinatura implements EnumComValor {
    INCOMPLETA("incompleta"),
    ATIVA("ativa"),
    PAST_DUE("past-due"),
    CANCELAMENTO_AGENDADO("cancelamento-agendado"),
    CANCELADA("cancelada");

    private final String valor;

    StatusAssinatura(String valor) {
        this.valor = valor;
    }

    @Override
    public String valor() {
        return valor;
    }
}
