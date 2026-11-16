package br.com.sauf_api.simulado;

import br.com.sauf_api.questao.AreaQuestao;
import br.com.sauf_api.questao.Banca;
import java.time.Instant;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        Integer acertos,
        Integer erros,
        Integer emBranco,
        Double porcentagemAcerto,
        int duracaoSegundos,
        List<AreaQuestao> areas,
        // Quantas questões de cada área o simulado teve, para repetir um personalizado (D-97)
        Map<String, Integer> questoesPorArea,
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
                s.getAcertos(),
                s.getErros(),
                s.getEmBranco(),
                concluido ? Porcentagem.de(s.getAcertos(), itens.size()) : null,
                itens.stream().mapToInt(SimuladoQuestao::getTempoSegundos).sum(),
                s.areasEmOrdem(),
                questoesPorArea(itens),
                concluido ? DesempenhoArea.de(itens) : List.of());
    }

    // Chave pelo valor da área ("lc"), na ordem lc, ch, cn, mt
    private static Map<String, Integer> questoesPorArea(List<SimuladoQuestao> itens) {
        Map<AreaQuestao, Integer> porArea = new EnumMap<>(AreaQuestao.class);
        itens.forEach(q -> porArea.merge(q.getQuestao().getArea(), 1, Integer::sum));
        Map<String, Integer> porValor = new LinkedHashMap<>();
        porArea.forEach((area, total) -> porValor.put(area.valor(), total));
        return porValor;
    }
}
