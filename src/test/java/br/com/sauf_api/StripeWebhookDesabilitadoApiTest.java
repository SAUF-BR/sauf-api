package br.com.sauf_api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "STRIPE_WEBHOOK_SECRET=")
@ActiveProfiles("test")
@AutoConfigureMockMvc
class StripeWebhookDesabilitadoApiTest {

    @Autowired MockMvc mvc;

    @Test
    void semSegredoNaoAceitaNenhumEvento() throws Exception {
        mvc.perform(post("/api/v1/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=1,v1=qualquer")
                        .content("{\"id\":\"evt_1\",\"type\":\"charge.refunded\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Webhooks do Stripe desabilitados"));
    }
}
