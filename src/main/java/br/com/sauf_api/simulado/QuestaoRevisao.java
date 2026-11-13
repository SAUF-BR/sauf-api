package br.com.sauf_api.simulado;

import br.com.sauf_api.questao.AreaQuestao;
import br.com.sauf_api.questao.Questao;
import java.util.List;

/** Questão na revisão: aqui, e só aqui, o gabarito aparece. */
public record QuestaoRevisao(
        int ordem,
        AreaQuestao area,
        int ano,
        int numero,
        String textoBase,
        String comando,
        List<AlternativaSimulado> alternativas,
        ResultadoQuestao resultado,
        boolean tempoEsgotado,
        String alternativaMarcada,
        String alternativaCorreta,
        int tempoSegundos) {

    static QuestaoRevisao de(SimuladoQuestao sq) {
        Questao q = sq.getQuestao();
        return new QuestaoRevisao(
                sq.getOrdem(),
                q.getArea(),
                q.getProva().getAno(),
                q.getNumero(),
                q.getTextoBase() == null ? null : q.getTextoBase().getConteudo(),
                q.getComando(),
                QuestaoSimulado.alternativas(q),
                sq.resultado(),
                sq.getSituacao() == SituacaoQuestao.TEMPO_ESGOTADO,
                sq.getAlternativaMarcada(),
                sq.rotuloCorreto(),
                sq.getTempoSegundos());
    }
}
