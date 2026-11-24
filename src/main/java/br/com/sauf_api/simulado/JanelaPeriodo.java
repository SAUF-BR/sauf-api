package br.com.sauf_api.simulado;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * Intervalo de dias (inclusivo) de uma estatística, nos dias de America/Sao_Paulo (D-84).
 * MOVEL termina hoje e tem o tamanho do período (7, 30 ou 365 dias); CALENDARIO é a semana
 * (segunda a domingo), o mês ou o ano corrente.
 */
public record JanelaPeriodo(Modo modo, PeriodoCronometro periodo, LocalDate inicio, LocalDate fim) {

    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    public enum Modo {
        MOVEL,
        CALENDARIO
    }

    public static LocalDate hoje(Clock clock) {
        return clock.instant().atZone(FUSO).toLocalDate();
    }

    public static JanelaPeriodo movel(PeriodoCronometro periodo, LocalDate hoje) {
        return new JanelaPeriodo(Modo.MOVEL, periodo, hoje.minusDays(periodo.dias() - 1L), hoje);
    }

    public static JanelaPeriodo calendario(PeriodoCronometro periodo, LocalDate hoje) {
        return switch (periodo) {
            case SEMANA -> {
                LocalDate segunda = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                yield new JanelaPeriodo(Modo.CALENDARIO, periodo, segunda, segunda.plusDays(6));
            }
            case MES -> new JanelaPeriodo(
                    Modo.CALENDARIO, periodo, hoje.withDayOfMonth(1), hoje.with(TemporalAdjusters.lastDayOfMonth()));
            case ANO -> new JanelaPeriodo(
                    Modo.CALENDARIO, periodo, hoje.withDayOfYear(1), hoje.with(TemporalAdjusters.lastDayOfYear()));
        };
    }

    /** A janela imediatamente antes desta, do mesmo tamanho (ou a semana, o mês ou o ano anterior). */
    public JanelaPeriodo anterior() {
        if (modo == Modo.MOVEL) {
            return new JanelaPeriodo(modo, periodo, inicio.minusDays(periodo.dias()), inicio.minusDays(1));
        }
        return calendario(periodo, inicio.minusDays(1));
    }

    public boolean contem(LocalDate dia) {
        return !dia.isBefore(inicio) && !dia.isAfter(fim);
    }

    public int dias() {
        return (int) ChronoUnit.DAYS.between(inicio, fim) + 1;
    }

    /** Primeiro instante da janela. */
    public Instant desde() {
        return inicio.atStartOfDay(FUSO).toInstant();
    }

    /** Primeiro instante depois da janela (limite exclusivo). */
    public Instant ate() {
        return fim.plusDays(1).atStartOfDay(FUSO).toInstant();
    }
}
