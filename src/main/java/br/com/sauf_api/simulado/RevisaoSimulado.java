package br.com.sauf_api.simulado;

import br.com.sauf_api.questao.AreaQuestao;
import br.com.sauf_api.questao.Banca;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RevisaoSimulado(
        UUID id,
        String titulo,
        Banca banca,
        TamanhoSimulado tamanho,
        List<AreaQuestao> areas,
        boolean cronometrado,
        Instant iniciadoEm,
        Instant finalizadoEm,
        int totalQuestoes,
        int acertos,
        int erros,
        int emBranco,
        double porcentagemAcerto,
        int duracaoSegundos,
        List<DesempenhoArea> porArea,
        List<QuestaoRevisao> questoes) {

    static RevisaoSimulado de(Simulado s, String titulo) {
        List<SimuladoQuestao> itens = s.questoesEmOrdem();
        return new RevisaoSimulado(
                s.getId(),
                titulo,
                s.getBanca(),
                s.getTamanho(),
                s.areasEmOrdem(),
                s.isCronometrado(),
                s.getIniciadoEm(),
                s.getFinalizadoEm(),
                itens.size(),
                s.getAcertos(),
                s.getErros(),
                s.getEmBranco(),
                Porcentagem.de(s.getAcertos(), itens.size()),
                itens.stream().mapToInt(SimuladoQuestao::getTempoSegundos).sum(),
                DesempenhoArea.de(itens),
                itens.stream().map(QuestaoRevisao::de).toList());
    }
}
