package br.com.sauf_api.simulado;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class JanelaPeriodoTest {

    private static LocalDate dia(String data) {
        return LocalDate.parse(data);
    }

    @Test
    void hojeUsaOFusoDeSaoPaulo() {
        // 02:30Z do dia 14 ainda é 23:30 do dia 13 em São Paulo
        Clock clock = Clock.fixed(Instant.parse("2026-11-14T02:30:00Z"), ZoneOffset.UTC);
        assertThat(JanelaPeriodo.hoje(clock)).isEqualTo(dia("2026-11-13"));
    }

    @Test
    void movelTerminaHojeEAnteriorEncostaNela() {
        JanelaPeriodo mes = JanelaPeriodo.movel(PeriodoCronometro.MES, dia("2026-03-01"));
        assertThat(mes.inicio()).isEqualTo(dia("2026-01-31"));
        assertThat(mes.fim()).isEqualTo(dia("2026-03-01"));
        assertThat(mes.dias()).isEqualTo(30);

        JanelaPeriodo anterior = mes.anterior();
        assertThat(anterior.inicio()).isEqualTo(dia("2026-01-01"));
        assertThat(anterior.fim()).isEqualTo(dia("2026-01-30"));
    }

    @Test
    void semanaDoCalendarioVaiDeSegundaADomingo() {
        // 2026-11-15 é domingo
        JanelaPeriodo semana = JanelaPeriodo.calendario(PeriodoCronometro.SEMANA, dia("2026-11-15"));
        assertThat(semana.inicio()).isEqualTo(dia("2026-11-09"));
        assertThat(semana.fim()).isEqualTo(dia("2026-11-15"));

        JanelaPeriodo segunda = JanelaPeriodo.calendario(PeriodoCronometro.SEMANA, dia("2026-11-16"));
        assertThat(segunda.inicio()).isEqualTo(dia("2026-11-16"));
        assertThat(segunda.anterior().inicio()).isEqualTo(dia("2026-11-09"));
    }

    @Test
    void mesDoCalendarioViraParaOMesAnteriorInteiro() {
        JanelaPeriodo marco = JanelaPeriodo.calendario(PeriodoCronometro.MES, dia("2028-03-31"));
        assertThat(marco.inicio()).isEqualTo(dia("2028-03-01"));
        assertThat(marco.fim()).isEqualTo(dia("2028-03-31"));

        JanelaPeriodo fevereiro = marco.anterior();
        assertThat(fevereiro.inicio()).isEqualTo(dia("2028-02-01"));
        assertThat(fevereiro.fim()).isEqualTo(dia("2028-02-29"));
    }

    @Test
    void anoDoCalendarioEOAnterior() {
        JanelaPeriodo ano = JanelaPeriodo.calendario(PeriodoCronometro.ANO, dia("2026-01-01"));
        assertThat(ano.fim()).isEqualTo(dia("2026-12-31"));
        assertThat(ano.anterior().inicio()).isEqualTo(dia("2025-01-01"));
        assertThat(ano.anterior().fim()).isEqualTo(dia("2025-12-31"));
    }

    @Test
    void limitesEmInstanteSeguemOFuso() {
        JanelaPeriodo dia = new JanelaPeriodo(
                JanelaPeriodo.Modo.MOVEL, PeriodoCronometro.SEMANA, dia("2026-11-13"), dia("2026-11-13"));
        assertThat(dia.desde()).isEqualTo(Instant.parse("2026-11-13T03:00:00Z"));
        assertThat(dia.ate()).isEqualTo(Instant.parse("2026-11-14T03:00:00Z"));
    }
}
