package br.com.sauf_api.simulado;

import java.time.LocalDate;
import java.util.List;

/** Tela "Ranking": tempo de estudo na semana, no mês ou no ano do calendário (D-87 a D-92). */
public record DadosRanking(
        PeriodoCronometro periodo,
        Intervalo janela,
        int totalParticipantes,
        List<Podio> podio,
        Colocacao colocacao,
        Percentil percentil,
        Estatisticas estatisticas) {

    public record Intervalo(LocalDate inicio, LocalDate fim) {}

    /** Posição real no ranking: o pódio pode ter o 1º e o 3º sem o 2º (D-88). */
    public record Podio(int posicao, String nome, String cidade, String uf, String fotoUrl, long segundos, long simulados) {}

    /** Nula quando o aluno não tem tempo na janela; {@code anterior} nulo se não tinha na janela anterior. */
    public record Colocacao(int posicao, Integer anterior) {}

    /** Entre os primeiros {@code percentual}% da região (UF do perfil ou Brasil). */
    public record Percentil(int percentual, String regiao) {}

    public record Estatisticas(long segundos, long simulados, int ofensiva) {}
}
