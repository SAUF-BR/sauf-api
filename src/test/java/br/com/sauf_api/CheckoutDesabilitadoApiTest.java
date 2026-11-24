package br.com.sauf_api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sauf_api.auth.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CheckoutDesabilitadoApiTest {

    @Autowired MockMvc mvc;
    @Autowired TokenService tokens;

    @Test
    void semChaveDoStripeResponde503() throws Exception {
        mvc.perform(post("/api/v1/assinatura/checkout")
                        .cookie(Sessao.cookie(tokens, Sessao.USUARIO_TESTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plano\":\"plus\",\"aceiteTermo\":true}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("Pagamentos desabilitados"));
    }
}
