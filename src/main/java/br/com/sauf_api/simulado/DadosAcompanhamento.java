package br.com.sauf_api.simulado;

import br.com.sauf_api.questao.AreaQuestao;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Tela "Meu acompanhamento". Os comparativos trazem a janela atual e a anterior do mesmo
 * tamanho; o front calcula o texto da variação. Porcentagens com 1 casa decimal.
 */
public record DadosAcompanhamento(
        PeriodoCronometro periodo,
        Intervalo janela,
        Resumo resumo,
        Evolucao evolucao,
        Distribuicao distribuicao,
        List<Area> areas,
        List<Foco> focos,
        Constancia constancia) {

    public record Intervalo(LocalDate inicio, LocalDate fim) {}

    /** {@code atual} ou {@code anterior} nulos quando a janela não teve simulado concluído. */
    public record Porcentagens(Double atual, Double anterior) {}

    public record Contagens(long atual, long anterior) {}

    public record Resumo(
            Porcentagens aproveitamento,
            Contagens questoesRespondidas,
            Contagens simuladosConcluidos,
            Contagens acertos) {}

    /** {@code mediaPlataforma} é nula com menos de 5 alunos na janela. */
    public record Evolucao(Double mediaPlataforma, List<PontoEvolucao> simulados) {}

    public record PontoEvolucao(UUID id, OffsetDateTime finalizadoEm, double aproveitamento) {}

    public record Distribuicao(Resultados atual, Resultados anterior) {}

    public record Resultados(long acertos, long erros, long emBranco) {}

    public record Area(AreaQuestao area, Porcentagens aproveitamento, long respondidas) {}

    /** Sem habilidade quando o foco é a área inteira (poucas respostas por habilidade). */
    public record Foco(int ordem, AreaQuestao area, String habilidade, String descricao, long erros, long respondidas) {}

    public record Constancia(int diasAtivos, int diasNaJanela, int ofensiva, List<Bloco> semanas) {}

    /** Um dia (semana), 7 dias (mês) ou um mês (ano) do gráfico de constância. */
    public record Bloco(LocalDate inicio, int dias, int diasAtivos) {}
}
