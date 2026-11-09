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
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
    "STRIPE_PRICE_BASIC=price_basic",
    "STRIPE_PRICE_PLUS=price_plus",
    "STRIPE_PRICE_PREMIUM=",
    "STRIPE_URL_SUCESSO=http://front/sucesso",
    "STRIPE_URL_CANCELAMENTO=http://front/cancelado"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class CheckoutApiTest {

    private static final String URL_CHECKOUT = "https://checkout.stripe.com/c/pay/cs_test_1";

    @Autowired MockMvc mvc;
    @Autowired AssinaturaRepository assinaturas;
    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS) StripeClient stripe;

    @BeforeEach
    void stripeDevolveSessao() throws StripeException {
        Session sessao = mock(Session.class);
        when(sessao.getUrl()).thenReturn(URL_CHECKOUT);
        when(stripe.v1().checkout().sessions().create(any(SessionCreateParams.class))).thenReturn(sessao);
    }

    @Test
    void criaSessaoDeAssinaturaEDevolveAUrl() throws Exception {
        checkout("{\"plano\":\"plus\",\"aceiteTermo\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(URL_CHECKOUT));

        SessionCreateParams params = paramsEnviados();
        assertThat(params.getMode()).isEqualTo(SessionCreateParams.Mode.SUBSCRIPTION);
        assertThat(params.getClientReferenceId()).isEqualTo(UsuarioLogado.PROVISORIO.toString());
        assertThat(params.getLineItems()).hasSize(1);
        assertThat(params.getLineItems().get(0).getPrice()).isEqualTo("price_plus");
        assertThat(params.getLineItems().get(0).getQuantity()).isEqualTo(1L);
        assertThat(params.getSuccessUrl()).isEqualTo("http://front/sucesso");
        assertThat(params.getCancelUrl()).isEqualTo("http://front/cancelado");
        assertThat(params.getCustomer()).isNull();
    }

    @Test
    void levaUsuarioEAceiteDoTermoNosMetadadosDaAssinatura() throws Exception {
        checkout("{\"plano\":\"basic\",\"aceiteTermo\":true}").andExpect(status().isOk());

        assertThat(paramsEnviados().getSubscriptionData().getMetadata())
                .containsEntry("usuario_id", UsuarioLogado.PROVISORIO.toString())
                .containsEntry("aceite_termo_maior_idade", "true")
                .containsEntry("aceite_versao_termo", "v1")
                .containsKey("aceite_em");
    }

    @Test
    void naoCriaLinhaDeAssinaturaAntesDoPagamento() throws Exception {
        checkout("{\"plano\":\"basic\",\"aceiteTermo\":true}").andExpect(status().isOk());

        assertThat(assinaturas.findFirstByUsuarioIdOrderByCriadaEmDesc(UsuarioLogado.PROVISORIO)).isEmpty();
    }

    @Test
    void semAceiteDoTermoResponde400ENaoChamaOStripe() throws Exception {
        checkout("{\"plano\":\"basic\",\"aceiteTermo\":false}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.aceiteTermo").value("Aceite o termo para continuar"));
        checkout("{\"plano\":\"basic\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.aceiteTermo").value("Aceite o termo para continuar"));

        verify(stripe.v1().checkout().sessions(), never()).create(any(SessionCreateParams.class));
    }

    @Test
    void semPlanoOuComPlanoInexistenteResponde400() throws Exception {
        checkout("{\"aceiteTermo\":true}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.plano").value("Escolha um plano"));
        checkout("{\"plano\":\"ouro\",\"aceiteTermo\":true}").andExpect(status().isBadRequest());
    }

    @Test
    void planoSemPriceConfiguradoResponde503() throws Exception {
        checkout("{\"plano\":\"premium\",\"aceiteTermo\":true}")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("O plano Premium está indisponível para assinatura"));

        verify(stripe.v1().checkout().sessions(), never()).create(any(SessionCreateParams.class));
    }

    @Test
    void assinaturaAbertaNoStripeResponde409ENaoCobraDeNovo() throws Exception {
        for (StatusAssinatura status : new StatusAssinatura[] {
            StatusAssinatura.ATIVA, StatusAssinatura.PAST_DUE, StatusAssinatura.CANCELAMENTO_AGENDADO
        }) {
            Assinatura assinatura = doStripe(status, LocalDate.now().plusDays(10));

            checkout("{\"plano\":\"plus\",\"aceiteTermo\":true}").andExpect(status().isConflict());

            assinaturas.delete(assinatura);
            assinaturas.flush();
        }
        verify(stripe.v1().checkout().sessions(), never()).create(any(SessionCreateParams.class));
    }

    @Test
    void assinaturaAbertaComRenovaEmVencidoTambemResponde409() throws Exception {
        doStripe(StatusAssinatura.PAST_DUE, LocalDate.now().minusDays(3));

        checkout("{\"plano\":\"plus\",\"aceiteTermo\":true}").andExpect(status().isConflict());
    }

    @Test
    void assinaturaCanceladaPodeAssinarDeNovoReusandoOCustomer() throws Exception {
        doStripe(StatusAssinatura.CANCELADA, LocalDate.now().plusDays(10));

        checkout("{\"plano\":\"plus\",\"aceiteTermo\":true}").andExpect(status().isOk());

        assertThat(paramsEnviados().getCustomer()).isEqualTo("cus_1");
    }

    @Test
    void assinaturaSimuladaSemStripeNaoImpedeOCheckout() throws Exception {
        assinaturas.saveAndFlush(new Assinatura(UsuarioLogado.PROVISORIO, Plano.BASIC, LocalDate.now().plusDays(10)));

        checkout("{\"plano\":\"plus\",\"aceiteTermo\":true}").andExpect(status().isOk());

        assertThat(paramsEnviados().getCustomer()).isNull();
    }

    @Test
    void falhaDoStripeResponde502SemExporODetalhe() throws Exception {
        when(stripe.v1().checkout().sessions().create(any(SessionCreateParams.class)))
                .thenThrow(new ApiConnectionException("detalhe interno do Stripe"));

        checkout("{\"plano\":\"plus\",\"aceiteTermo\":true}")
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Não foi possível iniciar o pagamento"));
    }

    private Assinatura doStripe(StatusAssinatura status, LocalDate renovaEm) {
        Assinatura assinatura = new Assinatura(UsuarioLogado.PROVISORIO, Plano.BASIC, renovaEm);
        ReflectionTestUtils.setField(assinatura, "status", status);
        ReflectionTestUtils.setField(assinatura, "stripeCustomerId", "cus_1");
        ReflectionTestUtils.setField(assinatura, "stripeSubscriptionId", "sub_1");
        return assinaturas.saveAndFlush(assinatura);
    }

    private SessionCreateParams paramsEnviados() throws StripeException {
        ArgumentCaptor<SessionCreateParams> captor = ArgumentCaptor.forClass(SessionCreateParams.class);
        verify(stripe.v1().checkout().sessions()).create(captor.capture());
        return captor.getValue();
    }

    private ResultActions checkout(String corpo) throws Exception {
        return mvc.perform(post("/api/v1/assinatura/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }
}
