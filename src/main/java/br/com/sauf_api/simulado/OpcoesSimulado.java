package br.com.sauf_api.simulado;

import br.com.sauf_api.questao.AreaQuestao;
import br.com.sauf_api.questao.Banca;
import java.util.List;

public record OpcoesSimulado(
        List<TamanhoOpcao> tamanhos,
        int segundosPorQuestao,
        List<Integer> temposPorQuestao,
        List<BancaOpcao> bancas,
        List<AreaOpcao> areas) {

    public record TamanhoOpcao(TamanhoSimulado id, int questoes) {}

    public record BancaOpcao(Banca id, String nome) {}

    public record AreaOpcao(AreaQuestao id, String nome, long questoesDisponiveis) {}
}
