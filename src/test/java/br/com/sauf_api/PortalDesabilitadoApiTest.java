package br.com.sauf_api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sauf_api.auth.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PortalDesabilitadoApiTest {

    @Autowired MockMvc mvc;
    @Autowired TokenService tokens;

    @Test
    void semChaveDoStripeResponde503() throws Exception {
        mvc.perform(post("/api/v1/assinatura/portal").cookie(Sessao.cookie(tokens, Sessao.USUARIO_TESTE)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Pagamentos desabilitados"));
    }
}
