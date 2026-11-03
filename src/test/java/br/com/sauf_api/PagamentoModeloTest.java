package br.com.sauf_api;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.sauf_api.assinatura.Assinatura;
import br.com.sauf_api.assinatura.AssinaturaRepository;
import br.com.sauf_api.assinatura.Plano;
import br.com.sauf_api.assinatura.StatusAssinatura;
import br.com.sauf_api.pagamento.Pagamento;
import br.com.sauf_api.pagamento.PagamentoRepository;
import br.com.sauf_api.pagamento.StatusPagamento;
import br.com.sauf_api.pagamento.StripeEvent;
import br.com.sauf_api.pagamento.StripeEventRepository;
import br.com.sauf_api.pagamento.StripeProperties;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PagamentoModeloTest {

    @Autowired AssinaturaRepository assinaturas;
    @Autowired PagamentoRepository pagamentos;
    @Autowired StripeEventRepository eventos;
    @Autowired StripeProperties stripe;

    @Test
    void semChaveDoStripeOsPagamentosFicamDesabilitados() {
        assertThat(stripe.habilitado()).isFalse();
    }

    @Test
    void assinaturaNovaComecaSemVinculoComOStripe() {
        Assinatura assinatura = assinaturas.saveAndFlush(
                new Assinatura(UUID.randomUUID(), Plano.BASIC, LocalDate.now().plusMonths(1)));

        Assinatura lida = assinaturas.findById(assinatura.getId()).orElseThrow();
        assertThat(lida.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(lida.getStripeSubscriptionId()).isNull();
        assertThat(lida.isCancelarNoFimPeriodo()).isFalse();
    }

    @Test
    void guardaPagamentoDaAssinaturaPeloIdDaInvoice() {
        Assinatura assinatura = assinaturas.saveAndFlush(
                new Assinatura(UUID.randomUUID(), Plano.PLUS, LocalDate.now().plusMonths(1)));
        pagamentos.saveAndFlush(new Pagamento(assinatura.getId(), "in_teste_1", 1200, StatusPagamento.PAGO, Instant.now()));

        Pagamento lido = pagamentos.findByStripeInvoiceId("in_teste_1").orElseThrow();
        assertThat(lido.getAssinaturaId()).isEqualTo(assinatura.getId());
        assertThat(lido.getValorCentavos()).isEqualTo(1200);
        assertThat(lido.getStatus()).isEqualTo(StatusPagamento.PAGO);
    }

    @Test
    void eventoDoStripeUsaOIdDoProprioStripe() {
        eventos.saveAndFlush(new StripeEvent("evt_teste_1", "invoice.payment_succeeded"));

        assertThat(eventos.existsById("evt_teste_1")).isTrue();
        assertThat(eventos.findById("evt_teste_1").orElseThrow().getProcessadoEm()).isNull();
    }
}
