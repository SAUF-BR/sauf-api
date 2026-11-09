package br.com.sauf_api.pagamento;

import br.com.sauf_api.assinatura.Assinatura;
import br.com.sauf_api.assinatura.AssinaturaRepository;
import br.com.sauf_api.assinatura.Plano;
import br.com.sauf_api.assinatura.StatusAssinatura;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.InvoicePayment;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.param.InvoicePaymentListParams;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Do evento só se usa o id do objeto: o estado vem sempre do Stripe (D-76). Assim, a ordem de
 * chegada dos eventos e a versão da API do webhook não mudam o resultado.
 */
@Component
public class EventosStripe {

    private static final Logger log = LoggerFactory.getLogger(EventosStripe.class);

    private final ObjectProvider<StripeClient> stripe;
    private final AssinaturaRepository assinaturas;
    private final PagamentoRepository pagamentos;
    private final PlanosStripe planos;
    private final JsonMapper json;

    public EventosStripe(
            ObjectProvider<StripeClient> stripe,
            AssinaturaRepository assinaturas,
            PagamentoRepository pagamentos,
            PlanosStripe planos,
            JsonMapper json) {
        this.stripe = stripe;
        this.assinaturas = assinaturas;
        this.pagamentos = pagamentos;
        this.planos = planos;
        this.json = json;
    }

    public void tratar(Event evento) {
        try {
            switch (evento.getType()) {
                case "checkout.session.completed" -> checkoutConcluido(idDoObjeto(evento));
                case "customer.subscription.updated", "customer.subscription.deleted" ->
                    sincronizar(idDoObjeto(evento));
                case "invoice.payment_succeeded", "invoice.payment_failed" -> cobranca(idDoObjeto(evento));
                case "charge.refunded" -> reembolso(idDoObjeto(evento));
                default -> log.debug("Evento do Stripe sem tratamento: {}", evento.getType());
            }
        } catch (StripeException e) {
            log.error("Falha ao consultar o Stripe no evento {}: {}", evento.getId(), e.getMessage());
            throw new FalhaNoGatewayException("Não foi possível consultar o Stripe", e);
        }
    }

    private void checkoutConcluido(String sessaoId) throws StripeException {
        String assinaturaId = cliente().v1().checkout().sessions().retrieve(sessaoId).getSubscription();
        if (assinaturaId != null) {
            sincronizar(assinaturaId);
        }
    }

    private void cobranca(String invoiceId) throws StripeException {
        Invoice invoice = cliente().v1().invoices().retrieve(invoiceId);
        Optional<Assinatura> assinatura = assinaturaDa(invoice);
        if (assinatura.isPresent()) {
            registrarCobranca(assinatura.get(), invoice);
        }
    }

    private void reembolso(String chargeId) throws StripeException {
        Charge charge = cliente().v1().charges().retrieve(chargeId);
        if (!Boolean.TRUE.equals(charge.getRefunded())) {
            log.warn("Reembolso parcial ignorado: {}", chargeId);
            return;
        }
        if (charge.getPaymentIntent() == null) {
            return;
        }
        List<InvoicePayment> pagos = cliente().v1().invoicePayments().list(InvoicePaymentListParams.builder()
                .setPayment(InvoicePaymentListParams.Payment.builder()
                        .setType(InvoicePaymentListParams.Payment.Type.PAYMENT_INTENT)
                        .setPaymentIntent(charge.getPaymentIntent())
                        .build())
                .build()).getData();
        if (pagos.isEmpty()) {
            return;
        }
        Invoice invoice = cliente().v1().invoices().retrieve(pagos.get(0).getInvoice());
        Optional<Assinatura> assinatura = assinaturaDa(invoice);
        if (assinatura.isPresent()) {
            registrarCobranca(assinatura.get(), invoice).marcarReembolsado();
        }
    }

    private Optional<Assinatura> assinaturaDa(Invoice invoice) throws StripeException {
        if (invoice.getParent() == null || invoice.getParent().getSubscriptionDetails() == null) {
            return Optional.empty();
        }
        return sincronizar(invoice.getParent().getSubscriptionDetails().getSubscription());
    }

    private Optional<Assinatura> sincronizar(String assinaturaId) throws StripeException {
        Subscription subscription = cliente().v1().subscriptions().retrieve(assinaturaId);
        SubscriptionItem item = subscription.getItems().getData().get(0);
        Plano plano = planos.planoDe(item.getPrice().getId()).orElseThrow(() -> new IllegalStateException(
                "Price do Stripe sem plano configurado: " + item.getPrice().getId()));
        LocalDate renovaEm = Instant.ofEpochSecond(item.getCurrentPeriodEnd()).atZone(ZoneOffset.UTC).toLocalDate();
        boolean cancelarNoFim = Boolean.TRUE.equals(subscription.getCancelAtPeriodEnd()) || subscription.getCancelAt() != null;

        Optional<Assinatura> existente = assinaturas.findByStripeSubscriptionId(assinaturaId);
        Assinatura assinatura;
        if (existente.isPresent()) {
            assinatura = existente.get();
        } else {
            String usuario = subscription.getMetadata() == null ? null : subscription.getMetadata().get("usuario_id");
            if (usuario == null) {
                log.warn("Assinatura do Stripe sem usuario_id, ignorada: {}", assinaturaId);
                return Optional.empty();
            }
            assinatura = nova(UUID.fromString(usuario), plano, renovaEm, subscription);
        }
        assinatura.sincronizarComStripe(
                plano, statusDe(subscription.getStatus(), cancelarNoFim), renovaEm, cancelarNoFim, subscription.getCustomer());
        return Optional.of(assinatura);
    }

    private Assinatura nova(UUID usuarioId, Plano plano, LocalDate renovaEm, Subscription subscription) {
        assinaturas.findFirstByUsuarioIdOrderByCriadaEmDesc(usuarioId)
                .filter(Assinatura::abertaNoStripe)
                .ifPresent(aberta -> log.error(
                        "Usuário {} com duas assinaturas abertas no Stripe: {} e {}",
                        usuarioId, aberta.getStripeSubscriptionId(), subscription.getId()));

        Map<String, String> metadados = subscription.getMetadata();
        Assinatura assinatura = new Assinatura(usuarioId, plano, renovaEm);
        assinatura.vincularAoStripe(subscription.getId());
        String aceiteEm = metadados.get("aceite_em");
        assinatura.registrarAceite(
                Boolean.parseBoolean(metadados.get("aceite_termo_maior_idade")),
                metadados.get("aceite_versao_termo"),
                aceiteEm == null ? null : Instant.parse(aceiteEm));
        return assinaturas.save(assinatura);
    }

    private Pagamento registrarCobranca(Assinatura assinatura, Invoice invoice) {
        boolean paga = "paid".equals(invoice.getStatus());
        int valor = Math.toIntExact(paga ? invoice.getAmountPaid() : invoice.getAmountDue());
        Instant pagaEm = paga && invoice.getStatusTransitions() != null && invoice.getStatusTransitions().getPaidAt() != null
                ? Instant.ofEpochSecond(invoice.getStatusTransitions().getPaidAt())
                : null;

        Optional<Pagamento> existente = pagamentos.findByStripeInvoiceId(invoice.getId());
        if (existente.isEmpty()) {
            return pagamentos.save(new Pagamento(
                    assinatura.getId(), invoice.getId(), valor,
                    paga ? StatusPagamento.PAGO : StatusPagamento.FALHOU, pagaEm));
        }
        if (paga) {
            existente.get().marcarPago(valor, pagaEm);
        }
        return existente.get();
    }

    private static StatusAssinatura statusDe(String status, boolean cancelarNoFim) {
        return switch (status) {
            case "active", "trialing" -> cancelarNoFim ? StatusAssinatura.CANCELAMENTO_AGENDADO : StatusAssinatura.ATIVA;
            case "past_due", "unpaid", "paused" -> StatusAssinatura.PAST_DUE;
            case "incomplete" -> StatusAssinatura.INCOMPLETA;
            case "canceled", "incomplete_expired" -> StatusAssinatura.CANCELADA;
            default -> throw new IllegalStateException("Status de assinatura do Stripe desconhecido: " + status);
        };
    }

    private String idDoObjeto(Event evento) {
        String id = json.readTree(evento.getDataObjectDeserializer().getRawJson()).path("id").asString();
        if (id == null || id.isBlank()) {
            throw new IllegalStateException("Evento do Stripe sem id do objeto: " + evento.getId());
        }
        return id;
    }

    private StripeClient cliente() {
        StripeClient cliente = stripe.getIfAvailable();
        if (cliente == null) {
            throw new PagamentoIndisponivelException("Pagamentos desabilitados");
        }
        return cliente;
    }
}
