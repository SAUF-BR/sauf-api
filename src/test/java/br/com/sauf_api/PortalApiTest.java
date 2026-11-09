package br.com.sauf_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sauf_api.assinatura.Assinatura;
import br.com.sauf_api.assinatura.AssinaturaRepository;
import br.com.sauf_api.assinatura.Plano;
import br.com.sauf_api.assinatura.StatusAssinatura;
import br.com.sauf_api.comum.UsuarioLogado;
import com.stripe.StripeClient;
import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.StripeException;
import com.stripe.model.billingportal.Session;
import com.stripe.param.billingportal.SessionCreateParams;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "STRIPE_URL_RETORNO_PORTAL=http://front/assinatura")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class PortalApiTest {

    private static final String URL_PORTAL = "https://billing.stripe.com/p/session/test_1";

    @Autowired MockMvc mvc;
    @Autowired AssinaturaRepository assinaturas;
    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS) StripeClient stripe;

    @BeforeEach
    void stripeDevolveSessao() throws StripeException {
        Session sessao = mock(Session.class);
        when(sessao.getUrl()).thenReturn(URL_PORTAL);
        when(stripe.v1().billingPortal().sessions().create(any(SessionCreateParams.class))).thenReturn(sessao);
    }

    @Test
    void abrePortalDoCustomerEDevolveAUrl() throws Exception {
        assinaturaNoStripe(StatusAssinatura.ATIVA);

        portal().andExpect(status().isOk()).andExpect(jsonPath("$.url").value(URL_PORTAL));

        ArgumentCaptor<SessionCreateParams> params = ArgumentCaptor.forClass(SessionCreateParams.class);
        verify(stripe.v1().billingPortal().sessions()).create(params.capture());
        assertThat(params.getValue().getCustomer()).isEqualTo("cus_1");
        assertThat(params.getValue().getReturnUrl()).isEqualTo("http://front/assinatura");
    }

    @Test
    void assinaturaCanceladaAindaAbreOPortalParaVerFaturas() throws Exception {
        assinaturaNoStripe(StatusAssinatura.CANCELADA);

        portal().andExpect(status().isOk());
    }

    @Test
    void semAssinaturaResponde409ENaoChamaOStripe() throws Exception {
        portal().andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Você ainda não tem uma assinatura para gerenciar"));

        verify(stripe.v1().billingPortal().sessions(), never()).create(any(SessionCreateParams.class));
    }

    @Test
    void assinaturaSimuladaSemCustomerResponde409() throws Exception {
        assinaturas.saveAndFlush(new Assinatura(UsuarioLogado.PROVISORIO, Plano.BASIC, LocalDate.now().plusDays(10)));

        portal().andExpect(status().isConflict());

        verify(stripe.v1().billingPortal().sessions(), never()).create(any(SessionCreateParams.class));
    }

    @Test
    void falhaDoStripeResponde502SemExporODetalhe() throws Exception {
        assinaturaNoStripe(StatusAssinatura.ATIVA);
        when(stripe.v1().billingPortal().sessions().create(any(SessionCreateParams.class)))
                .thenThrow(new ApiConnectionException("detalhe interno do Stripe"));

        portal().andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Não foi possível abrir o portal da assinatura"));
    }

    private void assinaturaNoStripe(StatusAssinatura status) {
        LocalDate renovaEm = LocalDate.now().plusDays(10);
        Assinatura assinatura = new Assinatura(UsuarioLogado.PROVISORIO, Plano.PLUS, renovaEm);
        assinatura.vincularAoStripe("sub_1");
        assinatura.sincronizarComStripe(Plano.PLUS, status, renovaEm, false, "cus_1");
        assinaturas.saveAndFlush(assinatura);
    }

    private ResultActions portal() throws Exception {
        return mvc.perform(post("/api/v1/assinatura/portal"));
    }
}
