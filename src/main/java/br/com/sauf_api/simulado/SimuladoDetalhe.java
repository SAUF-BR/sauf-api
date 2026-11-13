package br.com.sauf_api.simulado;

import br.com.sauf_api.questao.AreaQuestao;
import br.com.sauf_api.questao.Banca;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SimuladoDetalhe(
        UUID id,
        String titulo,
        Banca banca,
        TamanhoSimulado tamanho,
        List<AreaQuestao> areas,
        boolean cronometrado,
        Integer segundosPorQuestao,
        StatusSimulado status,
        Instant iniciadoEm,
        Instant finalizadoEm,
        int totalQuestoes,
        List<QuestaoSimulado> questoes) {

    static SimuladoDetalhe de(Simulado s, String titulo) {
        List<QuestaoSimulado> questoes =
                s.questoesEmOrdem().stream().map(QuestaoSimulado::de).toList();
        return new SimuladoDetalhe(
                s.getId(),
                titulo,
                s.getBanca(),
                s.getTamanho(),
                s.areasEmOrdem(),
                s.isCronometrado(),
                s.getSegundosPorQuestao(),
                s.getStatus(),
                s.getIniciadoEm(),
                s.getFinalizadoEm(),
                questoes.size(),
                questoes);
    }
}
