package br.com.sauf_api.simulado;

import br.com.sauf_api.questao.AreaQuestao;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record DesempenhoArea(AreaQuestao area, int acertos, int total) {

    /** Acertos e total por área, na ordem lc, ch, cn, mt; só vale para simulados finalizados. */
    static List<DesempenhoArea> de(Collection<SimuladoQuestao> questoes) {
        Map<AreaQuestao, int[]> contagens = new EnumMap<>(AreaQuestao.class);
        for (SimuladoQuestao sq : questoes) {
            int[] c = contagens.computeIfAbsent(sq.getQuestao().getArea(), a -> new int[2]);
            c[1]++;
            if (sq.resultado() == ResultadoQuestao.ACERTO) {
                c[0]++;
            }
        }
        return contagens.entrySet().stream()
                .map(e -> new DesempenhoArea(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();
    }
}
