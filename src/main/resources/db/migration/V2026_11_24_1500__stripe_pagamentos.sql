-- Integração com o Stripe (ADR-BE-016, TR-336).
-- Um ALTER por coluna para rodar igual no Postgres e no H2 dos testes.

-- Vínculo da assinatura com o Stripe e aceite do termo do checkout.
ALTER TABLE assinatura ADD COLUMN stripe_customer_id VARCHAR(255);
ALTER TABLE assinatura ADD COLUMN stripe_subscription_id VARCHAR(255);
ALTER TABLE assinatura ADD COLUMN cancelar_no_fim_periodo BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE assinatura ADD COLUMN aceite_termo_maior_idade BOOLEAN;
ALTER TABLE assinatura ADD COLUMN aceite_versao_termo VARCHAR(20);
ALTER TABLE assinatura ADD COLUMN aceite_em TIMESTAMP WITH TIME ZONE;

CREATE UNIQUE INDEX uq_assinatura_stripe_subscription ON assinatura (stripe_subscription_id);
CREATE INDEX idx_assinatura_stripe_customer ON assinatura (stripe_customer_id);

-- Idempotência dos webhooks: o id é o do evento no Stripe (evt_...), por isso texto e não UUID.
CREATE TABLE stripe_event (
    id            VARCHAR(255) PRIMARY KEY,
    tipo          VARCHAR(100) NOT NULL,
    recebido_em   TIMESTAMP WITH TIME ZONE NOT NULL,
    processado_em TIMESTAMP WITH TIME ZONE
);

-- Histórico de cobranças (uma linha por invoice do Stripe).
CREATE TABLE pagamento (
    id                UUID PRIMARY KEY,
    assinatura_id     UUID         NOT NULL REFERENCES assinatura (id),
    stripe_invoice_id VARCHAR(255) NOT NULL,
    valor_centavos    INTEGER      NOT NULL,
    status            VARCHAR(30)  NOT NULL,
    pago_em           TIMESTAMP WITH TIME ZONE,
    criado_em         TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uq_pagamento_stripe_invoice ON pagamento (stripe_invoice_id);
CREATE INDEX idx_pagamento_assinatura ON pagamento (assinatura_id, criado_em);
