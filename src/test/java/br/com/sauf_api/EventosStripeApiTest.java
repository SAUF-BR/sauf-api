package br.com.sauf_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sauf_api.assinatura.Assinatura;
import br.com.sauf_api.assinatura.AssinaturaRepository;
import br.com.sauf_api.assinatura.Plano;
import br.com.sauf_api.assinatura.StatusAssinatura;
import br.com.sauf_api.comum.UsuarioLogado;
import br.com.sauf_api.pagamento.Pagamento;
import br.com.sauf_api.pagamento.PagamentoRepository;
import br.com.sauf_api.pagamento.StatusPagamento;
import br.com.sauf_api.pagamento.StripeEventRepository;
import com.stripe.StripeClient;
import com.stripe.exception.ApiConnectionException;
import com.stripe.model.Charge;
import com.stripe.model.Invoice;
import com.stripe.model.InvoicePayment;
import com.stripe.model.Price;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.SubscriptionItemCollection;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.InvoicePaymentListParams;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

// Sem @Transactional: cada webhook precisa fazer o próprio commit ou rollback.
@SpringBootTest(properties = {
    "STRIPE_WEBHOOK_SECRET=" + EventosStripeApiTest.SEGREDO,
    "STRIPE_PRICE_BASIC=price_basic",
    "STRIPE_PRICE_PLUS=price_plus",
    "STRIPE_PRICE_PREMIUM=price_premium"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class EventosStripeApiTest {

    static final String SEGREDO = "whsec_teste";
    private static final UUID USUARIO = UsuarioLogado.PROVISORIO;
    private static final long FIM_DO_PERIODO = Instant.parse("2030-05-20T12:00:00Z").getEpochSecond();
    private static final LocalDate DATA_FIM_DO_PERIODO = LocalDate.of(2030, 5, 20);

    @Autowired MockMvc mvc;
    @Autowired AssinaturaRepository assinaturas;
    @Autowired PagamentoRepository pagamentos;
    @Autowired StripeEventRepository eventos;
    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS) StripeClient stripe;

    private int sequencia;

    @AfterEach
    void limpar() {
        pagamentos.deleteAll();
        assinaturas.deleteAll();
        eventos.deleteAll();
    }

    @Test
    void checkoutConcluidoCriaAssinaturaComDadosDoStripeEAceite() throws Exception {
        stripeTemSessao("cs_1", "sub_1");
        stripeTemAssinatura(subscription("sub_1", "active", "price_plus"));

        evento("checkout.session.completed", "cs_1").andExpect(status().isOk());

        Assinatura salva = assinatura("sub_1");
        assertThat(salva.getUsuarioId()).isEqualTo(USUARIO);
        assertThat(salva.getPlano()).isEqualTo(Plano.PLUS);
        assertThat(salva.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(salva.getRenovaEm()).isEqualTo(DATA_FIM_DO_PERIODO);
        assertThat(salva.getStripeCustomerId()).isEqualTo("cus_1");
        assertThat(salva.getAceiteTermoMaiorIdade()).isTrue();
        assertThat(salva.getAceiteVersaoTermo()).isEqualTo("v1");
        assertThat(salva.getAceiteEm()).isEqualTo(Instant.parse("2030-04-20T10:00:00Z"));
    }

    @Test
    void invoiceAntesDoCheckoutCriaUmaUnicaAssinaturaComOPagamento() throws Exception {
        stripeTemAssinatura(subscription("sub_1", "active", "price_plus"));
        stripeTemInvoice(invoice("in_1", "sub_1", "paid"));
        stripeTemSessao("cs_1", "sub_1");

        evento("invoice.payment_succeeded", "in_1").andExpect(status().isOk());
        evento("checkout.session.completed", "cs_1").andExpect(status().isOk());

        assertThat(assinaturas.count()).isEqualTo(1);
        Pagamento pagamento = pagamento("in_1");
        assertThat(pagamento.getAssinaturaId()).isEqualTo(assinatura("sub_1").getId());
        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.PAGO);
        assertThat(pagamento.getValorCentavos()).isEqualTo(1200);
        assertThat(pagamento.getPagoEm()).isEqualTo(Instant.ofEpochSecond(1_900_000_000L));
    }

    @Test
    void renovacaoPagaEstendeORenovaEmEVoltaParaAtiva() throws Exception {
        stripeTemAssinatura(subscription("sub_1", "past_due", "price_plus"));
        evento("customer.subscription.updated", "sub_1").andExpect(status().isOk());
        assertThat(assinatura("sub_1").getStatus()).isEqualTo(StatusAssinatura.PAST_DUE);

        Subscription renovada = subscription("sub_1", "active", "price_plus");
        renovada.getItems().getData().get(0).setCurrentPeriodEnd(FIM_DO_PERIODO + 30 * 86_400);
        stripeTemAssinatura(renovada);
        stripeTemInvoice(invoice("in_2", "sub_1", "paid"));
        evento("invoice.payment_succeeded", "in_2").andExpect(status().isOk());

        assertThat(assinatura("sub_1").getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(assinatura("sub_1").getRenovaEm()).isEqualTo(DATA_FIM_DO_PERIODO.plusDays(30));
    }

    @Test
    void cobrancaFalhaDepoisPagaAtualizaOMesmoPagamento() throws Exception {
        stripeTemAssinatura(subscription("sub_1", "past_due", "price_plus"));
        stripeTemInvoice(invoice("in_1", "sub_1", "open"));
        evento("invoice.payment_failed", "in_1").andExpect(status().isOk());

        assertThat(assinatura("sub_1").getStatus()).isEqualTo(StatusAssinatura.PAST_DUE);
        assertThat(pagamento("in_1").getStatus()).isEqualTo(StatusPagamento.FALHOU);
        assertThat(pagamento("in_1").getPagoEm()).isNull();
        assertThat(pagamento("in_1").getValorCentavos()).isEqualTo(1200);

        stripeTemAssinatura(subscription("sub_1", "active", "price_plus"));
        stripeTemInvoice(invoice("in_1", "sub_1", "paid"));
        evento("invoice.payment_succeeded", "in_1").andExpect(status().isOk());

        assertThat(pagamentos.count()).isEqualTo(1);
        assertThat(pagamento("in_1").getStatus()).isEqualTo(StatusPagamento.PAGO);
    }

    @Test
    void falhaQueChegaDepoisDoPagamentoNaoRegride() throws Exception {
        stripeTemAssinatura(subscription("sub_1", "active", "price_plus"));
        stripeTemInvoice(invoice("in_1", "sub_1", "paid"));

        evento("invoice.payment_succeeded", "in_1").andExpect(status().isOk());
        evento("invoice.payment_failed", "in_1").andExpect(status().isOk());

        assertThat(pagamento("in_1").getStatus()).isEqualTo(StatusPagamento.PAGO);
    }

    @Test
    void trocaDePlanoECancelamentoAgendadoVemDoStripe() throws Exception {
        stripeTemAssinatura(subscription("sub_1", "active", "price_basic"));
        evento("customer.subscription.updated", "sub_1").andExpect(status().isOk());

        Subscription alterada = subscription("sub_1", "active", "price_premium");
        alterada.setCancelAtPeriodEnd(true);
        stripeTemAssinatura(alterada);
        evento("customer.subscription.updated", "sub_1").andExpect(status().isOk());

        Assinatura salva = assinatura("sub_1");
        assertThat(salva.getPlano()).isEqualTo(Plano.PREMIUM);
        assertThat(salva.getStatus()).isEqualTo(StatusAssinatura.CANCELAMENTO_AGENDADO);
        assertThat(salva.isCancelarNoFimPeriodo()).isTrue();
    }

    @Test
    void cancelAtTambemContaComoCancelamentoAgendado() throws Exception {
        Subscription agendada = subscription("sub_1", "active", "price_basic");
        agendada.setCancelAt(FIM_DO_PERIODO);
        stripeTemAssinatura(agendada);

        evento("customer.subscription.updated", "sub_1").andExpect(status().isOk());

        assertThat(assinatura("sub_1").getStatus()).isEqualTo(StatusAssinatura.CANCELAMENTO_AGENDADO);
    }

    @Test
    void assinaturaEncerradaNoStripeFicaCanceladaESemAcesso() throws Exception {
        stripeTemAssinatura(subscription("sub_1", "canceled", "price_plus"));

        evento("customer.subscription.deleted", "sub_1").andExpect(status().isOk());

        assertThat(assinatura("sub_1").getStatus()).isEqualTo(StatusAssinatura.CANCELADA);
        mvc.perform(get("/api/v1/assinatura")).andExpect(status().isNoContent());
    }

    @Test
    void mapeiaOsStatusDoStripe() throws Exception {
        Map<String, StatusAssinatura> esperados = Map.of(
                "active", StatusAssinatura.ATIVA,
                "trialing", StatusAssinatura.ATIVA,
                "past_due", StatusAssinatura.PAST_DUE,
                "unpaid", StatusAssinatura.PAST_DUE,
                "paused", StatusAssinatura.PAST_DUE,
                "incomplete", StatusAssinatura.INCOMPLETA,
                "incomplete_expired", StatusAssinatura.CANCELADA,
                "canceled", StatusAssinatura.CANCELADA);

        for (var esperado : esperados.entrySet()) {
            stripeTemAssinatura(subscription("sub_1", esperado.getKey(), "price_plus"));
            evento("customer.subscription.updated", "sub_1").andExpect(status().isOk());
            assertThat(assinatura("sub_1").getStatus()).as(esperado.getKey()).isEqualTo(esperado.getValue());
        }
    }

    @Test
    void assinaturaIncompletaNaoContaComoVigente() throws Exception {
        stripeTemAssinatura(subscription("sub_1", "incomplete", "price_plus"));

        evento("customer.subscription.updated", "sub_1").andExpect(status().isOk());

        mvc.perform(get("/api/v1/assinatura")).andExpect(status().isNoContent());
    }

    @Test
    void reembolsoTotalMarcaOPagamentoESegueDefinitivo() throws Exception {
        stripeTemAssinatura(subscription("sub_1", "active", "price_plus"));
        stripeTemInvoice(invoice("in_1", "sub_1", "paid"));
        stripeTemCharge("ch_1", true);
        evento("invoice.payment_succeeded", "in_1").andExpect(status().isOk());

        evento("charge.refunded", "ch_1").andExpect(status().isOk());
        assertThat(pagamento("in_1").getStatus()).isEqualTo(StatusPagamento.REEMBOLSADO);
        assertThat(assinatura("sub_1").getStatus()).isEqualTo(StatusAssinatura.ATIVA);

        evento("invoice.payment_succeeded", "in_1").andExpect(status().isOk());
        assertThat(pagamento("in_1").getStatus()).isEqualTo(StatusPagamento.REEMBOLSADO);
    }

    @Test
    void reembolsoAntesDaInvoiceCriaOPagamentoReembolsado() throws Exception {
        stripeTemAssinatura(subscription("sub_1", "active", "price_plus"));
        stripeTemInvoice(invoice("in_1", "sub_1", "paid"));
        stripeTemCharge("ch_1", true);

        evento("charge.refunded", "ch_1").andExpect(status().isOk());

        assertThat(pagamento("in_1").getStatus()).isEqualTo(StatusPagamento.REEMBOLSADO);
    }

    @Test
    void reembolsoParcialNaoMudaOPagamento() throws Exception {
        stripeTemAssinatura(subscription("sub_1", "active", "price_plus"));
        stripeTemInvoice(invoice("in_1", "sub_1", "paid"));
        stripeTemCharge("ch_1", false);
        evento("invoice.payment_succeeded", "in_1").andExpect(status().isOk());

        evento("charge.refunded", "ch_1").andExpect(status().isOk());

        assertThat(pagamento("in_1").getStatus()).isEqualTo(StatusPagamento.PAGO);
    }

    @Test
    void assinaturaSemUsuarioNosMetadadosEhIgnorada() throws Exception {
        Subscription deOutroSistema = subscription("sub_outra", "active", "price_plus");
        deOutroSistema.setMetadata(Map.of());
        stripeTemAssinatura(deOutroSistema);

        evento("customer.subscription.updated", "sub_outra").andExpect(status().isOk());

        assertThat(assinaturas.count()).isZero();
        assertThat(eventos.count()).isEqualTo(1);
    }

    @Test
    void invoiceSemAssinaturaEhIgnorada() throws Exception {
        Invoice avulsa = invoice("in_avulsa", "sub_1", "paid");
        avulsa.setParent(null);
        stripeTemInvoice(avulsa);

        evento("invoice.payment_succeeded", "in_avulsa").andExpect(status().isOk());

        assertThat(pagamentos.count()).isZero();
        assertThat(assinaturas.count()).isZero();
    }

    @Test
    void segundaAssinaturaAbertaDoUsuarioEhRegistradaEPassaAValer() throws Exception {
        stripeTemAssinatura(subscription("sub_1", "active", "price_basic"));
        evento("customer.subscription.updated", "sub_1").andExpect(status().isOk());

        stripeTemAssinatura(subscription("sub_2", "active", "price_plus"));
        evento("customer.subscription.updated", "sub_2").andExpect(status().isOk());

        assertThat(assinaturas.count()).isEqualTo(2);
        assertThat(assinaturas.findFirstByUsuarioIdOrderByCriadaEmDesc(USUARIO).orElseThrow().getPlano())
                .isEqualTo(Plano.PLUS);
    }

    @Test
    void priceSemPlanoDesfazOEventoParaOStripeReenviar() throws Exception {
        stripeTemAssinatura(subscription("sub_1", "active", "price_desconhecido"));

        evento("customer.subscription.updated", "sub_1").andExpect(status().isInternalServerError());

        assertThat(assinaturas.count()).isZero();
        assertThat(eventos.count()).isZero();
    }

    @Test
    void falhaAoConsultarOStripeResponde502EPermiteReenvio() throws Exception {
        when(stripe.v1().subscriptions().retrieve("sub_1")).thenThrow(new ApiConnectionException("fora do ar"));
        String corpo = corpo("evt_reenviado", "customer.subscription.updated", "sub_1");

        enviar(corpo).andExpect(status().isBadGateway());
        assertThat(eventos.count()).isZero();

        org.mockito.Mockito.reset(stripe.v1().subscriptions());
        stripeTemAssinatura(subscription("sub_1", "active", "price_plus"));
        enviar(corpo).andExpect(status().isOk());

        assertThat(assinatura("sub_1").getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(eventos.count()).isEqualTo(1);
    }

    @Test
    void eventoSemTratamentoSoEhRegistrado() throws Exception {
        evento("customer.created", "cus_1").andExpect(status().isOk());

        assertThat(eventos.count()).isEqualTo(1);
        verify(stripe.v1().subscriptions(), never()).retrieve(anyString());
        verify(stripe.v1().invoicePayments(), never()).list(any(InvoicePaymentListParams.class));
    }

    private Assinatura assinatura(String id) {
        return assinaturas.findByStripeSubscriptionId(id).orElseThrow();
    }

    private Pagamento pagamento(String invoiceId) {
        return pagamentos.findByStripeInvoiceId(invoiceId).orElseThrow();
    }

    private void stripeTemSessao(String id, String subscription) throws Exception {
        Session sessao = new Session();
        sessao.setId(id);
        sessao.setSubscription(subscription);
        when(stripe.v1().checkout().sessions().retrieve(id)).thenReturn(sessao);
    }

    private void stripeTemAssinatura(Subscription subscription) throws Exception {
        when(stripe.v1().subscriptions().retrieve(subscription.getId())).thenReturn(subscription);
    }

    private void stripeTemInvoice(Invoice invoice) throws Exception {
        when(stripe.v1().invoices().retrieve(invoice.getId())).thenReturn(invoice);
    }

    private void stripeTemCharge(String id, boolean reembolsoTotal) throws Exception {
        Charge charge = new Charge();
        charge.setId(id);
        charge.setRefunded(reembolsoTotal);
        charge.setPaymentIntent("pi_1");
        when(stripe.v1().charges().retrieve(id)).thenReturn(charge);

        InvoicePayment pago = new InvoicePayment();
        pago.setInvoice("in_1");
        when(stripe.v1().invoicePayments().list(any(InvoicePaymentListParams.class)).getData())
                .thenReturn(List.of(pago));
    }

    private static Subscription subscription(String id, String status, String price) {
        Price preco = new Price();
        preco.setId(price);
        SubscriptionItem item = new SubscriptionItem();
        item.setPrice(preco);
        item.setCurrentPeriodEnd(FIM_DO_PERIODO);
        SubscriptionItemCollection itens = new SubscriptionItemCollection();
        itens.setData(List.of(item));

        Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.setStatus(status);
        subscription.setCustomer("cus_1");
        subscription.setItems(itens);
        subscription.setCancelAtPeriodEnd(false);
        subscription.setMetadata(Map.of(
                "usuario_id", USUARIO.toString(),
                "aceite_termo_maior_idade", "true",
                "aceite_versao_termo", "v1",
                "aceite_em", "2030-04-20T10:00:00Z"));
        return subscription;
    }

    private static Invoice invoice(String id, String subscription, String status) {
        Invoice.Parent.SubscriptionDetails detalhes = new Invoice.Parent.SubscriptionDetails();
        detalhes.setSubscription(subscription);
        Invoice.Parent parent = new Invoice.Parent();
        parent.setSubscriptionDetails(detalhes);
        Invoice.StatusTransitions transicoes = new Invoice.StatusTransitions();
        transicoes.setPaidAt("paid".equals(status) ? 1_900_000_000L : null);

        Invoice invoice = new Invoice();
        invoice.setId(id);
        invoice.setStatus(status);
        invoice.setParent(parent);
        invoice.setAmountDue(1200L);
        invoice.setAmountPaid("paid".equals(status) ? 1200L : 0L);
        invoice.setStatusTransitions(transicoes);
        return invoice;
    }

    private ResultActions evento(String tipo, String objetoId) throws Exception {
        return enviar(corpo("evt_" + (++sequencia), tipo, objetoId));
    }

    private static String corpo(String eventoId, String tipo, String objetoId) {
        return "{\"id\":\"" + eventoId + "\",\"object\":\"event\",\"type\":\"" + tipo
                + "\",\"data\":{\"object\":{\"id\":\"" + objetoId + "\"}}}";
    }

    private ResultActions enviar(String corpo) throws Exception {
        long agora = Instant.now().atZone(ZoneOffset.UTC).toEpochSecond();
        String assinatura = "t=" + agora + ",v1=" + Webhook.Util.computeHmacSha256(SEGREDO, agora + "." + corpo);
        return mvc.perform(post("/api/v1/webhooks/stripe")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", assinatura)
                .content(corpo.getBytes(StandardCharsets.UTF_8)));
    }
}
