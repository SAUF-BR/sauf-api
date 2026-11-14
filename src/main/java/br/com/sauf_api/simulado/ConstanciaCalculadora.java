package br.com.sauf_api.simulado;

import java.time.LocalDate;
import java.util.Set;

/** Regras de dias ativos e ofensiva, iguais no cronômetro e no acompanhamento (D-85). */
final class ConstanciaCalculadora {

    private ConstanciaCalculadora() {}

    record Ofensiva(int atual, int recorde) {}

    // Conta os dias seguidos até hoje; se hoje ainda não teve estudo, a sequência de ontem continua valendo.
    static int ofensivaAtual(Set<LocalDate> ativos, LocalDate hoje) {
        LocalDate dia = ativos.contains(hoje) ? hoje : hoje.minusDays(1);
        int atual = 0;
        while (ativos.contains(dia)) {
            atual++;
            dia = dia.minusDays(1);
        }
        return atual;
    }

    static Ofensiva ofensiva(Set<LocalDate> ativos, LocalDate hoje, LocalDate inicioRecorde) {
        int recorde = 0;
        int sequencia = 0;
        for (LocalDate d = inicioRecorde; !d.isAfter(hoje); d = d.plusDays(1)) {
            sequencia = ativos.contains(d) ? sequencia + 1 : 0;
            recorde = Math.max(recorde, sequencia);
        }
        return new Ofensiva(ofensivaAtual(ativos, hoje), recorde);
    }

    static int diasAtivos(Set<LocalDate> ativos, LocalDate de, LocalDate ate) {
        return (int) ativos.stream().filter(d -> !d.isBefore(de) && !d.isAfter(ate)).count();
    }
}
