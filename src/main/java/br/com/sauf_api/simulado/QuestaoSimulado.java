package br.com.sauf_api.simulado;

import br.com.sauf_api.questao.AreaQuestao;
import br.com.sauf_api.questao.Questao;
import java.util.Comparator;
import java.util.List;

/** Questão como o aluno vê durante o simulado: nunca traz o gabarito. */
public record QuestaoSimulado(
        int ordem,
        AreaQuestao area,
        int ano,
        int numero,
        String textoBase,
        String comando,
        List<AlternativaSimulado> alternativas,
        SituacaoQuestao situacao,
        String alternativaMarcada,
        int tempoSegundos) {

    static QuestaoSimulado de(SimuladoQuestao sq) {
        Questao q = sq.getQuestao();
        return new QuestaoSimulado(
                sq.getOrdem(),
                q.getArea(),
                q.getProva().getAno(),
                q.getNumero(),
                q.getTextoBase() == null ? null : q.getTextoBase().getConteudo(),
                q.getComando(),
                alternativas(q),
                sq.getSituacao(),
                sq.getAlternativaMarcada(),
                sq.getTempoSegundos());
    }

    static List<AlternativaSimulado> alternativas(Questao q) {
        return q.getAlternativas().stream()
                .sorted(Comparator.comparing(a -> a.getRotulo()))
                .map(AlternativaSimulado::de)
                .toList();
    }
}
