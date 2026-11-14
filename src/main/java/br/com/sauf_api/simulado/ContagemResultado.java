package br.com.sauf_api.simulado;

import br.com.sauf_api.questao.AreaQuestao;

/** Linha das consultas agregadas do acompanhamento: quantas questões de cada área com cada resultado. */
public record ContagemResultado(AreaQuestao area, Integer habilidade, SituacaoQuestao situacao, Boolean correta, long total) {

    boolean acerto() {
        return situacao == SituacaoQuestao.RESPONDIDA && Boolean.TRUE.equals(correta);
    }

    boolean erro() {
        return situacao == SituacaoQuestao.RESPONDIDA && !Boolean.TRUE.equals(correta);
    }
}
