package br.com.sauf_api.simulado;

import br.com.sauf_api.questao.AreaQuestao;

public record ResumoDesempenho(
        int simuladosFeitos, long questoesRespondidas, Double aproveitamentoMedio, MelhorArea melhorArea) {

    public record MelhorArea(AreaQuestao area, double porcentagem) {}
}
