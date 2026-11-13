package br.com.sauf_api.simulado;

import java.time.LocalDate;
import java.util.List;

/** Mesmo formato do DadosCronometro do front; todos os tempos em segundos. */
public record DadosCronometro(
        Comparativo tempoTotal,
        Comparativo tempoMedioQuestao,
        Comparativo diasAtivos,
        Ofensiva ofensiva,
        List<Dia> dias,
        List<Configuracao> porConfiguracao,
        List<Semana> horasPorSemana,
        List<Ritmo> ritmoPorCategoria) {

    /** {@code anterior} é nulo quando não há janela anterior (período ano). */
    public record Comparativo(int atual, Integer anterior) {}

    public record Ofensiva(int atual, int recorde) {}

    public record Dia(LocalDate data, int segundos) {}

    public record Configuracao(
            TamanhoSimulado tamanho,
            int questoes,
            int simulados,
            int mediaSegundos,
            int totalSegundos,
            int segundosPorQuestao,
            String observacao) {}

    public record Semana(String rotulo, int segundos) {}

    public record Ritmo(String categoria, int segundosPorQuestao) {}
}
