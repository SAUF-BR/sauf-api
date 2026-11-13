package br.com.sauf_api.simulado;

import br.com.sauf_api.questao.AreaQuestao;
import br.com.sauf_api.questao.Banca;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Item da lista de simulados; em andamento, as contagens e a nota vêm nulas e sem áreas. */
public record SimuladoResumo(
        UUID id,
        String titulo,
        Banca banca,
        StatusSimulado status,
        TamanhoSimulado tamanho,
        boolean cronometrado,
        Integer segundosPorQuestao,
        Instant iniciadoEm,
        Instant finalizadoEm,
        int totalQuestoes,
        int respondidas,
        int respondidasForaDoTempo,
        Integer acertos,
        Integer erros,
        Integer emBranco,
        Double porcentagemAcerto,
        int duracaoSegundos,
        List<AreaQuestao> areas,
        List<DesempenhoArea> porArea) {

    static SimuladoResumo de(Simulado s, String titulo) {
        List<SimuladoQuestao> itens = s.questoesEmOrdem();
        boolean concluido = s.concluido();
        return new SimuladoResumo(
                s.getId(),
                titulo,
                s.getBanca(),
                s.getStatus(),
                s.getTamanho(),
                s.isCronometrado(),
                s.getSegundosPorQuestao(),
                s.getIniciadoEm(),
                s.getFinalizadoEm(),
                itens.size(),
                (int) itens.stream()
                        .filter(q -> q.getSituacao() == SituacaoQuestao.RESPONDIDA)
                        .count(),
                (int) itens.stream().filter(SimuladoQuestao::respondidaForaDoTempo).count(),
                s.getAcertos(),
                s.getErros(),
                s.getEmBranco(),
                concluido ? Porcentagem.de(s.getAcertos(), itens.size()) : null,
                itens.stream().mapToInt(SimuladoQuestao::getTempoSegundos).sum(),
                s.areasEmOrdem(),
                concluido ? DesempenhoArea.de(itens) : List.of());
    }
}
