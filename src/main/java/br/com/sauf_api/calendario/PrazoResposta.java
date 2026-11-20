package br.com.sauf_api.calendario;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record PrazoResposta(
        UUID id,
        String titulo,
        @Schema(description = "O prazo: fim do período ou o dia do evento") LocalDate data,
        @Schema(description = "Quanto falta, em texto curto: \"Encerra em 5 dias\", \"Amanhã\", \"Começa em 10 dias\"")
        String descricao,
        TipoEvento tipo,
        LocalDate inicio,
        LocalDate fim,
        String fonteUrl,
        EventoResposta.InstituicaoEvento instituicao) {

    static PrazoResposta de(EventoCalendario e, LocalDate hoje) {
        return new PrazoResposta(
                e.getId(),
                e.getTitulo(),
                e.prazo(),
                quantoFalta(e, hoje),
                e.getTipo(),
                e.getInicio(),
                e.getFim(),
                e.getFonteUrl(),
                EventoResposta.InstituicaoEvento.de(e.getInstituicao()));
    }

    static String quantoFalta(EventoCalendario e, LocalDate hoje) {
        if (e.getFim() == null) {
            return emDias(ChronoUnit.DAYS.between(hoje, e.getInicio()), "Hoje", "Amanhã", "Em %d dias");
        }
        if (e.getInicio().isAfter(hoje)) {
            return emDias(ChronoUnit.DAYS.between(hoje, e.getInicio()), "Começa hoje", "Começa amanhã", "Começa em %d dias");
        }
        return emDias(ChronoUnit.DAYS.between(hoje, e.getFim()), "Encerra hoje", "Encerra amanhã", "Encerra em %d dias");
    }

    private static String emDias(long dias, String hoje, String amanha, String emN) {
        if (dias <= 0) {
            return hoje;
        }
        return dias == 1 ? amanha : emN.formatted(dias);
    }
}
