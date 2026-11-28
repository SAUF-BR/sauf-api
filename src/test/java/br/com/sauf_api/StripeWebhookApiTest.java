package br.com.sauf_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sauf_api.pagamento.EventosStripe;
import br.com.sauf_api.pagamento.StripeEvent;
import br.com.sauf_api.pagamento.StripeEventRepository;
import com.stripe.net.Webhook;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

// Sem @Transactional: os testes precisam ver o commit e o rollback do próprio webhook.
@SpringBootTest(properties = "STRIPE_WEBHOOK_SECRET=" + StripeWebhookApiTest.SEGREDO)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class StripeWebhookApiTest {

    static final String SEGREDO = "whsec_teste";

    @Autowired MockMvc mvc;
    @Autowired StripeEventRepository eventos;
    @MockitoBean EventosStripe tratador;

    @AfterEach
    void limpar() {
        eventos.deleteAll();
    }

    @Test
    void eventoAssinadoEhRegistradoETratadoSemApiKey() throws Exception {
        String corpo = evento("evt_1", "invoice.payment_succeeded");

        enviar(corpo, assinar(corpo, SEGREDO)).andExpect(status().isOk());

        StripeEvent salvo = eventos.findById("evt_1").orElseThrow();
        assertThat(salvo.getTipo()).isEqualTo("invoice.payment_succeeded");
        assertThat(salvo.getRecebidoEm()).isNotNull();
        assertThat(salvo.getProcessadoEm()).isNotNull();
        verify(tratador, times(1)).tratar(any());
    }

    @Test
    void eventoRepetidoResponde200ENaoEhTratadoDeNovo() throws Exception {
        String corpo = evento("evt_2", "invoice.payment_succeeded");

        enviar(corpo, assinar(corpo, SEGREDO)).andExpect(status().isOk());
        Instant primeiroRecebimento = eventos.findById("evt_2").orElseThrow().getRecebidoEm();
        enviar(corpo, assinar(corpo, SEGREDO)).andExpect(status().isOk());

        verify(tratador, times(1)).tratar(any());
        assertThat(eventos.count()).isEqualTo(1);
        assertThat(eventos.findById("evt_2").orElseThrow().getRecebidoEm()).isEqualTo(primeiroRecebimento);
    }

    @Test
    void falhaNoTratamentoDesfazORegistroParaOStripeReenviar() throws Exception {
        String corpo = evento("evt_3", "invoice.payment_succeeded");
        doThrow(new IllegalStateException("falhou")).when(tratador).tratar(any());

        enviar(corpo, assinar(corpo, SEGREDO)).andExpect(status().isInternalServerError());
        assertThat(eventos.existsById("evt_3")).isFalse();

        doNothing().when(tratador).tratar(any());
        enviar(corpo, assinar(corpo, SEGREDO)).andExpect(status().isOk());
        assertThat(eventos.findById("evt_3").orElseThrow().getProcessadoEm()).isNotNull();
    }

    @Test
    void verificaAAssinaturaSobreOsBytesExatosDoCorpo() throws Exception {
        String corpo = "{\"id\":\"evt_4\",\"object\":\"event\",\"type\":\"customer.subscription.updated\","
                + "\"data\":{\"object\":{\"object\":\"subscription\",\"description\":\"Assinatura de João\"}}}";

        enviar(corpo, assinar(corpo, SEGREDO)).andExpect(status().isOk());

        assertThat(eventos.existsById("evt_4")).isTrue();
    }

    @Test
    void assinaturaDeOutroSegredoResponde400() throws Exception {
        String corpo = evento("evt_5", "invoice.payment_succeeded");

        enviar(corpo, assinar(corpo, "whsec_outro"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Assinatura do webhook inválida"));

        naoRegistrou("evt_5");
    }

    @Test
    void corpoAlteradoDepoisDeAssinarResponde400() throws Exception {
        String assinatura = assinar(evento("evt_6", "invoice.payment_succeeded"), SEGREDO);

        enviar(evento("evt_6", "charge.refunded"), assinatura).andExpect(status().isBadRequest());

        naoRegistrou("evt_6");
    }

    @Test
    void assinaturaAntigaResponde400() throws Exception {
        String corpo = evento("evt_7", "invoice.payment_succeeded");
        long umaHoraAtras = Instant.now().minusSeconds(3600).getEpochSecond();
        String assinatura = "t=" + umaHoraAtras + ",v1=" + Webhook.Util.computeHmacSha256(SEGREDO, umaHoraAtras + "." + corpo);

        enviar(corpo, assinatura).andExpect(status().isBadRequest());

        naoRegistrou("evt_7");
    }

    @Test
    void semAssinaturaResponde400() throws Exception {
        mvc.perform(post("/api/v1/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evento("evt_8", "invoice.payment_succeeded")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Assinatura do webhook ausente"));

        naoRegistrou("evt_8");
    }

    @Test
    void corpoAssinadoMasInvalidoResponde400() throws Exception {
        for (String corpo : new String[] {"{nao-e-json", "{}"}) {
            enviar(corpo, assinar(corpo, SEGREDO)).andExpect(status().isBadRequest());
        }
        verify(tratador, never()).tratar(any());
        assertThat(eventos.count()).isZero();
    }

    private void naoRegistrou(String id) {
        assertThat(eventos.existsById(id)).isFalse();
        verify(tratador, never()).tratar(any());
    }

    private ResultActions enviar(String corpo, String assinatura) throws Exception {
        return mvc.perform(post("/api/v1/webhooks/stripe")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", assinatura)
                .content(corpo.getBytes(StandardCharsets.UTF_8)));
    }

    private static String assinar(String corpo, String segredo) throws Exception {
        long agora = Instant.now().getEpochSecond();
        return "t=" + agora + ",v1=" + Webhook.Util.computeHmacSha256(segredo, agora + "." + corpo);
    }

    private static String evento(String id, String tipo) {
        return "{\"id\":\"" + id + "\",\"object\":\"event\",\"type\":\"" + tipo + "\",\"data\":{\"object\":{}}}";
    }
}
