package br.com.sauf_api.calendario;

import br.com.sauf_api.instituicao.Instituicao;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.UUID;

public record EventoResposta(
        UUID id,
        String chave,
        String titulo,
        TipoEvento tipo,
        @Schema(description = "Dia em que o evento aparece no calendário: o início (igual a inicio)") LocalDate data,
        LocalDate inicio,
        @Schema(description = "Último dia do período; null quando o evento é de um dia só") LocalDate fim,
        String descricao,
        @Schema(description = "Página oficial de onde a data veio (edital, cronograma)") String fonteUrl,
        int anoReferencia,
        @Schema(description = "Universidade do evento; null nos eventos nacionais (ENEM, ProUni)")
        InstituicaoEvento instituicao) {

    public record InstituicaoEvento(UUID id, String sigla, String nome) {

        static InstituicaoEvento de(Instituicao i) {
            return i == null ? null : new InstituicaoEvento(i.getId(), i.getSigla(), i.getNome());
        }
    }

    public static EventoResposta de(EventoCalendario e) {
        return new EventoResposta(
                e.getId(),
                e.getChave(),
                e.getTitulo(),
                e.getTipo(),
                e.getInicio(),
                e.getInicio(),
                e.getFim(),
                e.getDescricao(),
                e.getFonteUrl(),
                e.getAnoReferencia(),
                InstituicaoEvento.de(e.getInstituicao()));
    }
}
