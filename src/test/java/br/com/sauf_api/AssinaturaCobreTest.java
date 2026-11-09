package br.com.sauf_api;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.sauf_api.assinatura.Assinatura;
import br.com.sauf_api.assinatura.Plano;
import br.com.sauf_api.assinatura.RecursoPlano;
import br.com.sauf_api.assinatura.StatusAssinatura;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssinaturaCobreTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 4);
    private static final LocalDate ONTEM = HOJE.minusDays(1);

    @Test
    void ativaLiberaMesmoComRenovaEmVencido() {
        assertThat(assinatura(StatusAssinatura.ATIVA, HOJE).cobre(RecursoPlano.SIMULADOS_ENEM, HOJE)).isTrue();
        assertThat(assinatura(StatusAssinatura.ATIVA, ONTEM).cobre(RecursoPlano.SIMULADOS_ENEM, HOJE)).isTrue();
    }

    @Test
    void pastDueLiberaAteORenovaEmInclusive() {
        assertThat(assinatura(StatusAssinatura.PAST_DUE, HOJE).cobre(RecursoPlano.SIMULADOS_ENEM, HOJE)).isTrue();
        assertThat(assinatura(StatusAssinatura.PAST_DUE, ONTEM).cobre(RecursoPlano.SIMULADOS_ENEM, HOJE)).isFalse();
    }

    @Test
    void cancelamentoAgendadoLiberaAteORenovaEmInclusive() {
        assertThat(assinatura(StatusAssinatura.CANCELAMENTO_AGENDADO, HOJE).cobre(RecursoPlano.SIMULADOS_ENEM, HOJE))
                .isTrue();
        assertThat(assinatura(StatusAssinatura.CANCELAMENTO_AGENDADO, ONTEM).cobre(RecursoPlano.SIMULADOS_ENEM, HOJE))
                .isFalse();
    }

    @Test
    void incompletaECanceladaBloqueiamMesmoDentroDoPeriodo() {
        LocalDate futuro = HOJE.plusDays(10);

        assertThat(assinatura(StatusAssinatura.INCOMPLETA, futuro).cobre(RecursoPlano.SIMULADOS_ENEM, HOJE)).isFalse();
        assertThat(assinatura(StatusAssinatura.CANCELADA, futuro).cobre(RecursoPlano.SIMULADOS_ENEM, HOJE)).isFalse();
    }

    @Test
    void recursoForaDoPlanoBloqueiaMesmoComStatusLiberado() {
        Assinatura basic = assinatura(StatusAssinatura.ATIVA, HOJE);

        assertThat(basic.cobre(RecursoPlano.SIMULADOS_VESTIBULARES, HOJE)).isFalse();
        assertThat(basic.cobre(RecursoPlano.DASHBOARD, HOJE)).isFalse();
    }

    private Assinatura assinatura(StatusAssinatura status, LocalDate renovaEm) {
        Assinatura assinatura = new Assinatura(UUID.randomUUID(), Plano.BASIC, renovaEm);
        assinatura.sincronizarComStripe(Plano.BASIC, status, renovaEm, false, "cus_1");
        return assinatura;
    }
}
